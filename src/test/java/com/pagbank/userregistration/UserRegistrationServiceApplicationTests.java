package com.pagbank.userregistration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Perfil "test" (T4.1) desabilita a exportação OTLP: sem collector local, evita
// tentativas de conexão/stack traces de exportador durante o build.
@SpringBootTest
@ActiveProfiles("test")
class UserRegistrationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
