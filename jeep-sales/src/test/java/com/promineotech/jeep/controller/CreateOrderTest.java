package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import lombok.Getter;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = {
		"classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
		"classpath:flyway/migrations/V1.1__Jeep_Data.sql"},
		config = @SqlConfig(encoding = "utf-8"))
class CreateOrderTest {
	@Autowired
	@Getter
	private TestRestTemplate restTemplate;
	
	@LocalServerPort
	private int serverPort;
	
	protected String getUriForOrders() {
		return String.format("http://localhost:%d/jeeps?order=%s", serverPort);
	}

	/**
	 * 
	 */
	@Test
	void testCreateOrderReturnsSuccess201() {
		//Given: an order as JSON
		String body = createOrderBody();
		String uri = getUriForOrders();
//		String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		
		HttpEntity<String> bodyEntity = new HttpEntity<>(body, headers);
		//When: the order is sent
		ResponseEntity<?> response = getRestTemplate().exchange(uri, HttpMethod.POST, bodyEntity, Object.class);
		 
		//Then: a 201 status is returned
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		
		//And: the returned order is correct
		
		
	}
	
	

	protected String createOrderBody() {
		// @formatter: off
		return "{\n"
			+ "  \"customer\":\"MORISON_LINA\",\n"
			+ "  \"model\":\"WRANGLER\",\n"
			+ "  \"trim\":\"Sport Altitude\",\n"
			+ "  \"doors\":4,\n"
			+ "  \"color\":\"EXT_NACHO\",\n"
			+ "  \"engine\":\"2_0_TURBO\",\n"
			+ "  \"tire\":\"35_TOYO\",\n"
			+ "  \"options\":[\n"
			+ "    \"DOOR_QUAD_4\",\n"
			+ "    \"EXT_AEV_LIFT\",\n"
			+ "    \"EXT_WARN_WINCH\",\n"
			+ "    \"EXT_WARN_BUMPER_FRONT\",\n"
			+ "    \"EXT_WARN_BUMPER_REAR\",\n"
			+ "    \"EXT_ARB_COMPRESSOR\",\n"
			+ "  ]\n"
			+ "}";
		// @formatter: on
	}
 
	
}
