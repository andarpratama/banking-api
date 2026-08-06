package com.company.banking.customer.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating customer profile.
 */
public class UpdateCustomerRequest {

    @NotBlank(message = "Full name cannot be blank")
    @Size(min = 1, max = 255, message = "Full name must be between 1 and 255 characters")
    private String fullName;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    public UpdateCustomerRequest() {
    }

    public UpdateCustomerRequest(String fullName, String phone, String address) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
