 package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import com.promineotech.jeep.Constants;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;
import com.promineotech.jeep.service.JeepSalesService;

import lombok.Getter;


class FetchJeepTest{
	
	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@ActiveProfiles("test")
	@Sql(scripts = {
			"classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
			"classpath:flyway/migrations/V1.1__Jeep_Data.sql"},
			config = @SqlConfig(encoding = "utf-8"))
	class TestsThatDoNotPolluteTheApplicationContext{
		@Autowired
		@Getter
		private TestRestTemplate restTemplate;
		
		@LocalServerPort
		private int serverPort;
		
		@Test
		void testDb() {
			}
		
		@Test
		void testThatJeepsAreReturnedWhenAValidModelAndTrimAreSupplied() {
			//Given: a valid model, trim, and URI
			JeepModel model = JeepModel.WRANGLER;
			String trim  = "Sport";
			String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);		
			//When: a connection is made to the URI
			ResponseEntity<List<Jeep>> response =
					getRestTemplate().exchange(uri, HttpMethod.GET, null, 
							new ParameterizedTypeReference<>() {});
			//Then: a success (OK - 200) status code is returned.
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			//And: the actual list returned is the same as the expected list
			List<Jeep> actual = response.getBody();
			List<Jeep> expected = buildExpected();
			
			assertThat(actual).isEqualTo(expected);
			}
		
		@ParameterizedTest
		@MethodSource("com.promineotech.jeep.controller.FetchJeepTest#parametersForInvalidInput")
		void testThatAnErrorMessageIsReturnedWhenAnInvalidValueIsSupplied(String model, String trim, String reason) {
			//Given: a valid model, trim, and URI
			String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);		
			//When: a connection is made to the URI
			ResponseEntity<Map<String, Object>> response =
					getRestTemplate().exchange(uri, HttpMethod.GET, null, 
							new ParameterizedTypeReference<>() {});
			//Then: a not found (400) status code is returned.
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			//And: an error message is returned
			Map<String, Object> error = response.getBody();
			
			assertErrorMessageValid(error, HttpStatus.BAD_REQUEST);
			}
		
		@Test
		void testThatAnErrorMessageIsReturnedWhenAnUnknownTrimIsSupplied() {
			//Given: a valid model, trim, and URI
			JeepModel model = JeepModel.WRANGLER;
			String trim  = "Unknown Value";
			String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);		
			//When: a connection is made to the URI
			ResponseEntity<Map<String, Object>> response =
					getRestTemplate().exchange(uri, HttpMethod.GET, null, 
							new ParameterizedTypeReference<>() {});
			//Then: a not found (404) status code is returned.
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
			//And: an error message is returned
			Map<String, Object> error = response.getBody();
			assertErrorMessageValid(error, HttpStatus.NOT_FOUND);
			} 
		} // end of non-pollution
	
	static Stream<Arguments> parametersForInvalidInput(){
		// @formatter: off
		return Stream.of(
				arguments("WRANGLER", "!@&(*!&%$(*", "Trim contains non-alpha-numeric characters"),
				arguments("WRANGLER", "C".repeat(Constants.TRIM_MAX_LENGTH +1), "Trim length too long"),
				arguments("INVALID", "Sport", "Model is not enum value ")
				);
		// @formatter: on
		}
	
	
	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@ActiveProfiles("test")
	@Sql(scripts = {
			"classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
			"classpath:flyway/migrations/V1.1__Jeep_Data.sql"},
			config = @SqlConfig(encoding = "utf-8"))
	class TestsThatPolluteTheApplicationContext{
		
		@Autowired
		@Getter
		private TestRestTemplate restTemplate;
		
		@LocalServerPort
		private int serverPort;
		
		@MockBean
		private JeepSalesService jeepSalesService;
		@Test
		void testThatAnUnplannedErrorResultsInA500Status() {
			//Given: a valid model, trim, and URI
			JeepModel model = JeepModel.WRANGLER;
			String trim  = "Invalid";
			String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);		
			
			doThrow(new RuntimeException("Ouch!")).when(jeepSalesService).fetchJeeps(model, trim);
			
			//When: a connection is made to the URI
			ResponseEntity<Map<String, Object>> response =
					getRestTemplate().exchange(uri, HttpMethod.GET, null, 
							new ParameterizedTypeReference<>() {});
			//Then: an internal server error (500) is returned
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			//And: an error message is returned
			Map<String, Object> error = response.getBody();
			assertErrorMessageValid(error, HttpStatus.INTERNAL_SERVER_ERROR);
			} 
		
		
	} // end of pollution
	
	
	
	 // end of tests
	
	protected List<Jeep> buildExpected() {
		List<Jeep> list = new LinkedList<>();
		// @formatter: off
		list.add(Jeep.builder()
				.modelId(JeepModel.WRANGLER)
				.trimLevel("Sport")
				.numDoors(4)
				.wheelSize(17)
				.basePrice(new BigDecimal("31975.00"))
				.build());
		list.add(Jeep.builder()
				.modelId(JeepModel.WRANGLER)
				.trimLevel("Sport")
				.numDoors(2)
				.wheelSize(17)
				.basePrice(new BigDecimal("28475.00"))
				.build());
		// @formatter: on
		
		Collections.sort(list);
		return list;
		}
	
	/**
	 * 
	 * @param error
	 * @param status
	 */
	protected void assertErrorMessageValid(Map<String, Object> error, HttpStatus status) {
		// @formatter: off
		assertThat(error)
			.containsKey("message")
			.containsEntry("status code", status.value())
			.containsEntry("uri", "/jeeps")
			.containsKey("timestamp")
			.containsEntry("reason", status.getReasonPhrase());
		
		// @formatter: on
	} 
} // end of class
