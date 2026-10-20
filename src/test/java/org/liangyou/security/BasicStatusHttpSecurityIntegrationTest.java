package org.liangyou.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real controller, persistence, and complete security chain, using an isolated database. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:status-http;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.cloud.nacos.config.enabled=false", "spring.config.import=", "security.jwt.secret=integration-secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class BasicStatusHttpSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void isolatedUnit() throws Exception {
        jdbc.execute("DROP ALL OBJECTS");
        String init = Files.readString(Path.of("sql/grain_oil_ims_init.sql"));
        jdbc.execute(init.substring(init.indexOf("CREATE TABLE base_unit ("), init.indexOf("CREATE TABLE base_product (")));
        jdbc.update("INSERT INTO base_unit(id,unit_code,unit_name,status) VALUES(1,'BAG','袋',1)");
    }

    @Test
    void putCannotBypassStatusPermissionAndDeniedMutationRemainsAtomic() throws Exception {
        String disabled = "{\"unitCode\":\"BAG\",\"unitName\":\"新名称\",\"status\":0}";
        var editor = user("editor").authorities(new SimpleGrantedAuthority("basic:unit:update"));
        mvc.perform(put("/api/v1/basic/units/1").with(editor).contentType(APPLICATION_JSON).content(disabled))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无权限访问"));
        assertEquals(1, jdbc.queryForObject("SELECT status FROM base_unit WHERE id=1", Integer.class));
        assertEquals("袋", jdbc.queryForObject("SELECT unit_name FROM base_unit WHERE id=1", String.class));

        mvc.perform(put("/api/v1/basic/units/1").with(editor).contentType(APPLICATION_JSON)
                        .content(disabled.replace("\"status\":0", "\"status\":1")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        assertEquals("新名称", jdbc.queryForObject("SELECT unit_name FROM base_unit WHERE id=1", String.class));

        mvc.perform(put("/api/v1/basic/units/1").with(user("manager").authorities(
                                new SimpleGrantedAuthority("basic:unit:update"), new SimpleGrantedAuthority("basic:unit:status")))
                        .contentType(APPLICATION_JSON).content(disabled))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        assertEquals(0, jdbc.queryForObject("SELECT status FROM base_unit WHERE id=1", Integer.class));
    }
}
