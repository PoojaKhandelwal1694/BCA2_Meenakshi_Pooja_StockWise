package com.stockwise.stockwise.supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierControllerTest {

    @Mock
    private SupplierRepository repository;

    @InjectMocks
    private SupplierController controller;


    // TEST 1: Retrieve all suppliers
    @Test
    void shouldReturnAllSuppliers() {

        Supplier supplier1 = new Supplier();
        supplier1.setSupplierName("ABC Traders");

        Supplier supplier2 = new Supplier();
        supplier2.setSupplierName("XYZ Suppliers");

        when(repository.findAll())
                .thenReturn(List.of(supplier1, supplier2));

        List<Supplier> result = controller.getAllSuppliers();

        assertEquals(2, result.size());
        assertEquals("ABC Traders", result.get(0).getSupplierName());
        assertEquals("XYZ Suppliers", result.get(1).getSupplierName());

        verify(repository, times(1)).findAll();
    }


    // TEST 2: Retrieve an existing supplier by ID
    @Test
    void shouldReturnSupplierWhenIdExists() {

        Supplier supplier = new Supplier();
        supplier.setSupplierName("ABC Traders");

        when(repository.findById(1))
                .thenReturn(Optional.of(supplier));

        ResponseEntity<Supplier> response =
                controller.getSupplier(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ABC Traders",
                response.getBody().getSupplierName());

        verify(repository).findById(1);
    }


    // TEST 3: Return 404 when supplier does not exist
    @Test
    void shouldReturnNotFoundWhenSupplierDoesNotExist() {

        when(repository.findById(999))
                .thenReturn(Optional.empty());

        ResponseEntity<Supplier> response =
                controller.getSupplier(999);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        verify(repository).findById(999);
    }


    // TEST 4: Reject a supplier with a blank name
    @Test
    void shouldRejectSupplierWithBlankName() {

        Supplier supplier = new Supplier();
        supplier.setSupplierName("   ");

        ResponseEntity<?> response =
                controller.addSupplier(supplier);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verify(repository, never()).save(any(Supplier.class));
    }


    // TEST 5: Save a valid supplier
    @Test
    void shouldSaveSupplierWithValidName() {

        Supplier supplier = new Supplier();
        supplier.setSupplierName("  ABC Traders  ");
        supplier.setCompanyName("ABC Company");
        supplier.setPhone("9876543210");
        supplier.setEmail("abc@example.com");
        supplier.setAddress("Jaipur");

        when(repository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response =
                controller.addSupplier(supplier);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        Supplier savedSupplier = (Supplier) response.getBody();

        assertEquals("ABC Traders", savedSupplier.getSupplierName());
        assertEquals("ABC Company", savedSupplier.getCompanyName());
        assertEquals("9876543210", savedSupplier.getPhone());
        assertEquals("abc@example.com", savedSupplier.getEmail());

        verify(repository, times(1)).save(supplier);
    }
}