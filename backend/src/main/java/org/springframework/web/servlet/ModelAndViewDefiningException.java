package org.springframework.web.servlet;

import javax.servlet.ServletException;

/**
 * Compatibility shim for frameworks/components still referencing this legacy type.
 */
public class ModelAndViewDefiningException extends ServletException {

    private static final long serialVersionUID = 1L;

    private final ModelAndView modelAndView;

    public ModelAndViewDefiningException(ModelAndView modelAndView) {
        this.modelAndView = modelAndView;
    }

    public ModelAndView getModelAndView() {
        return modelAndView;
    }
}
