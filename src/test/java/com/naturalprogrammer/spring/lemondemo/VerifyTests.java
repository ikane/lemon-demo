package com.naturalprogrammer.spring.lemondemo;

import static com.jayway.restassured.RestAssured.given;
import static com.naturalprogrammer.spring.lemondemo.testutil.MyTestUtil.hasErrors;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.restassured.response.Response;
import com.naturalprogrammer.spring.lemon.domain.AbstractUser.Role;
import com.naturalprogrammer.spring.lemondemo.entities.User;
import com.naturalprogrammer.spring.lemondemo.repositories.UserRepository;

/**
 * Test cases for verifying user
 * 
 * @author Sanjay Patel
 *
 */
public class VerifyTests extends AbstractTests {
	
	@Autowired
	private UserRepository userRepository;
	
    /**
     * Can verify user. Re-verifying should throw error. 
     */
	@Test
    public void canVerifyUser() {
    	
    	// Sign a user up
    	User signedUp = SignupTests.signupUser1(filters); 
		
		// Ensure that his verification code is not null
    	Assert.assertNotNull("Verification code should not be null, but it is.", signedUp.getVerificationCode());
    	
		// Verify
    	verify(signedUp.getVerificationCode())
		.then()
			.statusCode(200)
			.body("id", equalTo(signedUp.getId().intValue())) // same user is returned
			.body("roles", not(hasItem(Role.UNVERIFIED))) // now verified
			.body("goodUser", equalTo(true));  // now a good user 	

    	// Ensure that he is still logged in
    	BasicTests.getContext(filters)
    	.then()
    		.root("user")
			.body("id", equalTo(signedUp.getId().intValue())) // same user is returned
			.body("roles", not(hasItem(Role.UNVERIFIED)))  // now verified
			.body("goodUser", equalTo(true)); // now a good user
    	
		// Re-verifying should throw error
    	verify(signedUp.getVerificationCode())
		.then()
			.statusCode(400)
			.body("exception", equalTo("MultiErrorException"))
			.body("errors", hasErrors(null, "com.naturalprogrammer.spring.alreadyVerified"));
    	
    	// Fetch the verified User from database
    	User verifiedUser = userRepository.findOne(signedUp.getId());
    	
    	// It's verificationCode should now be null
    	Assert.assertNull("After verifiaction, the verification code must set to null, but it is "
    			+ verifiedUser.getVerificationCode(), verifiedUser.getVerificationCode());
    	
    	// And, roles should not contain UNVERIFIED
    	Assert.assertTrue("After verification, roles should not contain UNVERIFIED",
    			!verifiedUser.getRoles().contains(Role.UNVERIFIED));    	
    }
    
    /**
     * Try with a wrong verification code
     */
	@Test
    public void wrongVerificationCode() {
    	
    	// Sign up a user
    	SignupTests.signupUser1(filters); 
    	
		// Try verifying with wrong verification code
    	verify("a-wrong-verification-code")
		.then()
			.statusCode(400)
			.body("exception", equalTo("MultiErrorException"))
			.body("errors", hasErrors(null, "com.naturalprogrammer.spring.wrong.verificationCode"));
    }
    
    /**
     * Try without logged in
     */
	@Test
    public void verifyWithoutLoggingIn() {
    	
    	// Sign a user up
    	User signedUp = SignupTests.signupUser1(filters); 
    	
    	// Then, log him out
    	BasicTests.logout(filters);
    	
		// Try verifying
    	verify(signedUp.getVerificationCode())
		.then()
			.statusCode(403)
			.body("exception", equalTo("AccessDeniedException"));
    }

    /**
     * Helper method for verifying
     * 
     * @param verificationCode
     * @return the Response
     */
	private Response verify(String verificationCode) {
    	return given().spec(filters)
        		.pathParam("verificationCode", verificationCode)
        		.post("/api/core/users/{verificationCode}/verify");	
    }
    
}
