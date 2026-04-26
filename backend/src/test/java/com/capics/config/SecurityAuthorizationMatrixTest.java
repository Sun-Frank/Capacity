package com.capics.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "plan_user", roles = {"PLAN"})
    void planRole_ShouldAllowMasterDataRead_ButBlockWrite() throws Exception {
        MvcResult readResult = mockMvc.perform(get("/api/products/__security_probe__"))
                .andReturn();
        assertNotEquals(401, readResult.getResponse().getStatus());
        assertNotEquals(403, readResult.getResponse().getStatus());

        mockMvc.perform(post("/api/products/__security_probe__"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "master_user", roles = {"MASTERDATA"})
    void masterDataRole_ShouldAllowMasterDataReadAndWrite() throws Exception {
        MvcResult readResult = mockMvc.perform(get("/api/products/__security_probe__"))
                .andReturn();
        assertNotEquals(401, readResult.getResponse().getStatus());
        assertNotEquals(403, readResult.getResponse().getStatus());

        MvcResult writeResult = mockMvc.perform(post("/api/products/__security_probe__"))
                .andReturn();
        assertNotEquals(401, writeResult.getResponse().getStatus());
        assertNotEquals(403, writeResult.getResponse().getStatus());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void adminRole_ShouldAllowSystemApi() throws Exception {
        MvcResult usersApiResult = mockMvc.perform(get("/api/users/__security_probe__"))
                .andReturn();
        assertNotEquals(401, usersApiResult.getResponse().getStatus());
        assertNotEquals(403, usersApiResult.getResponse().getStatus());
    }

    @Test
    @WithMockUser(username = "plan_user", roles = {"PLAN"})
    void planRole_ShouldBlockSystemApi() throws Exception {
        mockMvc.perform(get("/api/users/__security_probe__"))
                .andExpect(status().isForbidden());
    }
}
