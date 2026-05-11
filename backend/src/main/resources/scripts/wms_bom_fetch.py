#!/usr/bin/env python3
"""Fetch BOM exports from WMS and convert them to CAPICS routing rows.

Required third-party packages on the deployment host:
  selenium ddddocr pandas openpyxl pillow

The workflow mirrors the original BOM下载.py:
  1. login WMS with captcha OCR
  2. enter the BOM query menu
  3. query/export each finished item number
  4. merge exported xlsx files
  5. convert parent-child BOM rows to CAPICS columns
"""

import argparse
import glob
import json
import os
import sys
import time
from pathlib import Path
from urllib.parse import urlparse

import pandas as pd

try:
    from PIL import Image

    if not hasattr(Image, "ANTIALIAS"):
        Image.ANTIALIAS = Image.LANCZOS
except ImportError:
    pass

from ddddocr import DdddOcr
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait


USERNAME_XPATH = '//*[@id="txtUsername"]'
PASSWORD_XPATH = '//*[@id="txtPassword"]'
CAPTCHA_IMG_XPATH = '//*[@id="img-captcha"]'
CAPTCHA_INPUT_XPATH = '//*[@id="txtValid"]'
LOGIN_BTN_XPATH = '//*[@id="IbLogin"]'
TARGET_MENU_XPATH = '//*[@id="common_menu"]/a[4]'
MP_INPUT_ID = "ctl03_tbMaterialGroup_suggest"
BOM_INPUT_ID = "ctl03_tbBom1_suggest"
SEARCH_BTN_ID = "ctl03_btnSearch"
EXPORT_BTN_ID = "ctl03_btnExport"


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True)
    parser.add_argument("--materials", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--work-dir", required=True)
    return parser.parse_args()


def load_inputs(args):
    with open(args.config, "r", encoding="utf-8") as handle:
        config = json.load(handle)
    with open(args.materials, "r", encoding="utf-8") as handle:
        materials = [line.strip() for line in handle if line.strip()]
    if not materials:
        raise RuntimeError("No materials provided")
    return config, materials


def build_driver(login_url, download_dir):
    parsed = urlparse(login_url)
    base_origin = f"{parsed.scheme}://{parsed.netloc}"
    chrome_driver = os.environ.get("WMS_CHROMEDRIVER", "chromedriver")

    options = Options()
    options.add_argument("--headless=new")
    options.add_argument("--disable-gpu")
    options.add_argument("--disable-extensions")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--allow-running-insecure-content")
    options.add_argument(f"--unsafely-treat-insecure-origin-as-secure={base_origin}")
    options.add_experimental_option(
        "prefs",
        {
            "download.default_directory": str(download_dir),
            "download.prompt_for_download": False,
            "download.directory_upgrade": True,
            "safebrowsing.enabled": True,
        },
    )
    return webdriver.Chrome(service=Service(chrome_driver), options=options)


def login(driver, wait, config, work_dir):
    login_url = config["loginUrl"]
    driver.get(login_url)
    wait.until(EC.presence_of_element_located((By.XPATH, USERNAME_XPATH))).send_keys(config["username"])
    driver.find_element(By.XPATH, PASSWORD_XPATH).send_keys(config["password"])

    ocr = DdddOcr(show_ad=False)
    for attempt in range(1, 6):
        try:
            if attempt > 1:
                driver.refresh()
                time.sleep(1.5)

            captcha = wait.until(EC.visibility_of_element_located((By.XPATH, CAPTCHA_IMG_XPATH)))
            time.sleep(0.5)
            img_path = work_dir / f"captcha_{attempt}.png"
            captcha.screenshot(str(img_path))
            code = ocr.classification(img_path.read_bytes()).strip()
            if len(code) != 4:
                raise RuntimeError("Invalid captcha length")

            driver.find_element(By.XPATH, CAPTCHA_INPUT_XPATH).clear()
            driver.find_element(By.XPATH, CAPTCHA_INPUT_XPATH).send_keys(code)
            driver.find_element(By.XPATH, LOGIN_BTN_XPATH).click()
            time.sleep(2)

            if login_url not in driver.current_url:
                print("WMS login succeeded", flush=True)
                return
        except Exception as exc:
            print(f"WMS login attempt {attempt} failed: {exc}", flush=True)

    raise RuntimeError("WMS login failed after 5 captcha attempts")


def switch_to_frame_with_element(driver, wait, by, value):
    driver.switch_to.default_content()
    for iframe in driver.find_elements(By.TAG_NAME, "iframe"):
        driver.switch_to.frame(iframe)
        try:
            return wait.until(EC.element_to_be_clickable((by, value)))
        except Exception:
            driver.switch_to.default_content()
    return wait.until(EC.element_to_be_clickable((by, value)))


def open_bom_menu(driver, wait):
    wait.until(EC.element_to_be_clickable((By.XPATH, TARGET_MENU_XPATH))).click()
    time.sleep(1)
    wait.until(EC.frame_to_be_available_and_switch_to_it((By.TAG_NAME, "iframe")))
    mp_input = wait.until(EC.element_to_be_clickable((By.ID, MP_INPUT_ID)))
    mp_input.clear()
    mp_input.send_keys("MP")
    driver.switch_to.default_content()


def export_materials(driver, wait, materials):
    for index, material in enumerate(materials, start=1):
        input_box = switch_to_frame_with_element(driver, wait, By.ID, BOM_INPUT_ID)
        input_box.clear()
        input_box.send_keys(material)
        wait.until(EC.element_to_be_clickable((By.ID, SEARCH_BTN_ID))).click()
        time.sleep(0.3)
        wait.until(EC.element_to_be_clickable((By.ID, EXPORT_BTN_ID))).click()
        print(f"Exported {index}/{len(materials)} {material}", flush=True)
        time.sleep(0.3)


def wait_downloads(download_dir):
    while any(str(path).endswith(".crdownload") for path in os.listdir(download_dir)):
        time.sleep(0.5)


def find_column(columns, aliases):
    normalized = {str(col).strip(): col for col in columns}
    for alias in aliases:
        if alias in normalized:
            return normalized[alias]
    lowered = {str(col).strip().lower(): col for col in columns}
    for alias in aliases:
        found = lowered.get(alias.lower())
        if found is not None:
            return found
    raise RuntimeError(f"Missing required column, expected one of: {', '.join(aliases)}")


def level_to_number(value):
    text = "" if pd.isna(value) else str(value).strip()
    if not text:
        return 0
    if "." in text:
        return text.count(".")
    try:
        return int(float(text))
    except Exception:
        return 0


def convert_exports(download_dir, output_file, input_materials):
    excel_files = [
        path
        for path in glob.glob(str(download_dir / "*.xlsx"))
        if os.path.abspath(path) != os.path.abspath(output_file)
    ]
    if not excel_files:
        raise RuntimeError("No exported Excel files were downloaded from WMS")

    frames = [pd.read_excel(path) for path in excel_files]
    merged = pd.concat(frames, ignore_index=True)

    finished_col = find_column(merged.columns, ["成品", "成品物料", "成品物料号", "Finished Item"])
    component_col = find_column(merged.columns, ["物料号", "组件", "组件物料号", "Component", "Component Code"])
    line_col = find_column(merged.columns, ["生产线", "Production Line", "Line"])
    level_col = find_column(merged.columns, ["层次", "层级", "BOM层级", "Level"])

    merged = merged[merged[level_col].astype(str).str.strip() != "0"]

    parent_child_map = {}
    finished_products = set()
    for _, row in merged.iterrows():
        parent = "" if pd.isna(row[finished_col]) else str(row[finished_col]).strip()
        child = "" if pd.isna(row[component_col]) else str(row[component_col]).strip()
        line = "" if pd.isna(row[line_col]) else str(row[line_col]).strip()
        level = level_to_number(row[level_col])
        if not parent or not child:
            continue
        parent_child_map.setdefault(parent, []).append((child, line, level))
        if level == 1:
            finished_products.add(parent)

    if not finished_products:
        finished_products = set(input_materials)

    results = []
    for finished in sorted(finished_products):
        stack = list(parent_child_map.get(finished, []))
        visited = set()
        while stack:
            child, line, level = stack.pop()
            key = (finished, child, line, level)
            if key in visited:
                continue
            visited.add(key)
            results.append(
                {
                    "成品物料号": finished,
                    "成品描述": "",
                    "组件物料号": child,
                    "生产线": line,
                    "BOM层级": max(level, 1),
                    "BOM用量": 1,
                }
            )
            stack.extend(parent_child_map.get(child, []))

    pd.DataFrame(results).to_excel(output_file, index=False)
    print(f"Wrote {len(results)} routing rows to {output_file}", flush=True)


def main():
    args = parse_args()
    work_dir = Path(args.work_dir)
    download_dir = work_dir / "downloads"
    download_dir.mkdir(parents=True, exist_ok=True)
    config, materials = load_inputs(args)

    driver = build_driver(config["loginUrl"], download_dir)
    wait = WebDriverWait(driver, 15)
    try:
        login(driver, wait, config, work_dir)
        open_bom_menu(driver, wait)
        export_materials(driver, wait, materials)
        wait_downloads(download_dir)
    finally:
        driver.quit()

    convert_exports(download_dir, args.output, materials)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(str(exc), file=sys.stderr, flush=True)
        sys.exit(1)
