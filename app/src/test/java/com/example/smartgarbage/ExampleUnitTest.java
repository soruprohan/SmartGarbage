package com.example.smartgarbage;

import org.junit.Test;

import static org.junit.Assert.*;

import com.example.smartgarbage.data.model.Bin;
import com.example.smartgarbage.data.model.ChangePasswordRequest;
import com.example.smartgarbage.data.model.DriverHomeResponse;
import com.example.smartgarbage.data.model.DriverProfile;
import com.example.smartgarbage.data.model.ForgotPasswordRequest;
import com.example.smartgarbage.data.model.LoginRequest;
import com.example.smartgarbage.data.model.SendMessageRequest;
import com.example.smartgarbage.data.model.UpdateDriverRequest;
import com.example.smartgarbage.utils.Resource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for SmartGarbage model classes and utility wrappers.
 * These run on the host JVM — no Android device/emulator needed.
 */
public class ExampleUnitTest {

    // ──────────────────────────────────────────────
    //  Helper: set a private field via reflection
    // ──────────────────────────────────────────────
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ──────────────────────────────────────────────
    //  1. Bin – getFillPercentage() normal case
    // ──────────────────────────────────────────────
    @Test
    public void bin_fillPercentage_normalCase() throws Exception {
        Bin bin = new Bin();
        setField(bin, "capacity", 100);
        setField(bin, "current_level", 75);

        assertEquals(75, bin.getFillPercentage());
    }

    // ──────────────────────────────────────────────
    //  2. Bin – getFillPercentage() with zero capacity
    // ──────────────────────────────────────────────
    @Test
    public void bin_fillPercentage_zeroCapacity() throws Exception {
        Bin bin = new Bin();
        setField(bin, "capacity", 0);
        setField(bin, "current_level", 50);

        assertEquals(0, bin.getFillPercentage());
    }

    // ──────────────────────────────────────────────
    //  3. Bin – getFillPercentage() when completely full
    // ──────────────────────────────────────────────
    @Test
    public void bin_fillPercentage_full() throws Exception {
        Bin bin = new Bin();
        setField(bin, "capacity", 200);
        setField(bin, "current_level", 200);

        assertEquals(100, bin.getFillPercentage());
    }

    // ──────────────────────────────────────────────
    //  4. Bin – getter values after reflection set
    // ──────────────────────────────────────────────
    @Test
    public void bin_gettersReturnCorrectValues() throws Exception {
        Bin bin = new Bin();
        setField(bin, "id", 42);
        setField(bin, "name", "Bin-A");
        setField(bin, "location", "Block-7");
        setField(bin, "status", "active");
        setField(bin, "latitude", 23.8103);
        setField(bin, "longitude", 90.4125);

        assertEquals(42, bin.getId());
        assertEquals("Bin-A", bin.getName());
        assertEquals("Block-7", bin.getLocation());
        assertEquals("active", bin.getStatus());
        assertEquals(23.8103, bin.getLatitude(), 0.0001);
        assertEquals(90.4125, bin.getLongitude(), 0.0001);
    }

    // ──────────────────────────────────────────────
    //  5. LoginRequest – constructor & getters
    // ──────────────────────────────────────────────
    @Test
    public void loginRequest_constructorSetsFields() {
        LoginRequest req = new LoginRequest("driver@test.com", "secret123");

        assertEquals("driver@test.com", req.getEmail());
        assertEquals("secret123", req.getPassword());
    }

    // ──────────────────────────────────────────────
    //  6. ChangePasswordRequest – stores password
    // ──────────────────────────────────────────────
    @Test
    public void changePasswordRequest_storesPassword() {
        ChangePasswordRequest req = new ChangePasswordRequest("newPass456");

        assertEquals("newPass456", req.getPassword());
    }

    // ──────────────────────────────────────────────
    //  7. ForgotPasswordRequest – stores email
    // ──────────────────────────────────────────────
    @Test
    public void forgotPasswordRequest_storesEmail() {
        ForgotPasswordRequest req = new ForgotPasswordRequest("forgot@test.com");

        assertEquals("forgot@test.com", req.getEmail());
    }

    // ──────────────────────────────────────────────
    //  8. UpdateDriverRequest – constructor & getters
    // ──────────────────────────────────────────────
    @Test
    public void updateDriverRequest_constructorSetsFields() {
        UpdateDriverRequest req = new UpdateDriverRequest("Rahim", "01712345678");

        assertEquals("Rahim", req.getName());
        assertEquals("01712345678", req.getPhone());
    }

    // ──────────────────────────────────────────────
    //  9. Resource – factory methods & status helpers
    // ──────────────────────────────────────────────
    @Test
    public void resource_factoryMethodsSetCorrectStatus() {
        Resource<String> loading = Resource.loading();
        Resource<String> success = Resource.success("data");
        Resource<String> error   = Resource.error("oops", null);

        assertTrue(loading.isLoading());
        assertFalse(loading.isSuccess());
        assertNull(loading.data);

        assertTrue(success.isSuccess());
        assertEquals("data", success.data);

        assertTrue(error.isError());
        assertEquals("oops", error.message);
        assertNull(error.data);
    }

    // ──────────────────────────────────────────────
    //  10. Resource – error can carry partial data
    // ──────────────────────────────────────────────
    @Test
    public void resource_errorCanCarryData() {
        Resource<Integer> res = Resource.error("timeout", 42);

        assertTrue(res.isError());
        assertEquals("timeout", res.message);
        assertEquals(Integer.valueOf(42), res.data);
    }

    // ──────────────────────────────────────────────
    //  11. DriverProfile – getAssignedBinCount with nulls
    // ──────────────────────────────────────────────
    @Test
    public void driverProfile_assignedBinCount_handlesNulls() throws Exception {
        DriverProfile profile = new DriverProfile();

        // null bins list → 0
        assertEquals(0, profile.getAssignedBinCount());

        // list with a null entry and a valid bin
        Bin validBin = new Bin();
        setField(validBin, "id", 5);

        Bin zeroBin = new Bin(); // id defaults to 0

        Field binsField = DriverProfile.class.getDeclaredField("bins");
        binsField.setAccessible(true);
        binsField.set(profile, Arrays.asList(null, validBin, zeroBin));

        assertEquals(1, profile.getAssignedBinCount());
    }

    // ──────────────────────────────────────────────
    //  12. DriverHomeResponse – nested Driver object
    // ──────────────────────────────────────────────
    @Test
    public void driverHomeResponse_nestedDriverFields() throws Exception {
        DriverHomeResponse response = new DriverHomeResponse();
        setField(response, "message", "Welcome");

        DriverHomeResponse.Driver driver = new DriverHomeResponse.Driver();
        Field idField = DriverHomeResponse.Driver.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.setInt(driver, 7);

        Field nameField = DriverHomeResponse.Driver.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(driver, "Karim");

        Field emailField = DriverHomeResponse.Driver.class.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(driver, "karim@test.com");

        setField(response, "driver", driver);

        assertEquals("Welcome", response.getMessage());
        assertNotNull(response.getDriver());
        assertEquals(7, response.getDriver().getId());
        assertEquals("Karim", response.getDriver().getName());
        assertEquals("karim@test.com", response.getDriver().getEmail());
    }
}