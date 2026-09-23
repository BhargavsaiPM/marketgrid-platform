package com.marketgrid.vendorservice.service;

import com.marketgrid.vendorservice.entity.Vendor;
import com.marketgrid.vendorservice.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private VendorService vendorService;

    private Vendor sampleVendor;

    @BeforeEach
    void setUp() {
        sampleVendor = new Vendor(10L, "Acme Tech", "Gadgets", "+1-555-1234", "123 Tech Way");
        sampleVendor.setId(100L);
    }

    @Test
    void testRegisterVendor_Success() {
        when(vendorRepository.existsByUserId(10L)).thenReturn(false);
        when(vendorRepository.save(any(Vendor.class))).thenReturn(sampleVendor);

        Vendor created = vendorService.registerVendor(10L, "Acme Tech", "Gadgets", "+1-555-1234", "123 Tech Way");

        assertNotNull(created);
        assertEquals("Acme Tech", created.getBusinessName());
        assertFalse(created.isApproved()); // initially unapproved
        verify(vendorRepository, times(1)).save(any(Vendor.class));
    }

    @Test
    void testRegisterVendor_DuplicateUser() {
        when(vendorRepository.existsByUserId(10L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                vendorService.registerVendor(10L, "Acme Tech", "Gadgets", "+1-555-1234", "123 Tech Way"));

        assertTrue(ex.getMessage().contains("already exists for userId"));
        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    @Test
    void testUpdateVendor_Success() {
        when(vendorRepository.findById(100L)).thenReturn(Optional.of(sampleVendor));
        when(vendorRepository.save(any(Vendor.class))).thenReturn(sampleVendor);

        Vendor updated = vendorService.updateVendor(100L, 10L, "Acme Tech Updated", "New Desc", "+1-555-9999", "New Address");

        assertNotNull(updated);
        assertEquals("Acme Tech Updated", updated.getBusinessName());
        verify(vendorRepository, times(1)).save(sampleVendor);
    }

    @Test
    void testUpdateVendor_UnauthorizedOtherUser() {
        when(vendorRepository.findById(100L)).thenReturn(Optional.of(sampleVendor));

        // Requesting user is 99L, but owner is 10L
        SecurityException ex = assertThrows(SecurityException.class, () ->
                vendorService.updateVendor(100L, 99L, "Tampered Name", null, null, null));

        assertTrue(ex.getMessage().contains("You do not own this vendor profile"));
        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    @Test
    void testDeleteVendor_UnauthorizedOtherUser() {
        when(vendorRepository.findById(100L)).thenReturn(Optional.of(sampleVendor));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                vendorService.deleteVendor(100L, 99L));

        assertTrue(ex.getMessage().contains("You do not own this vendor profile"));
        verify(vendorRepository, never()).delete(any(Vendor.class));
    }

    @Test
    void testApproveVendor_Success() {
        when(vendorRepository.findById(100L)).thenReturn(Optional.of(sampleVendor));
        when(vendorRepository.save(any(Vendor.class))).thenReturn(sampleVendor);

        Vendor approved = vendorService.approveVendor(100L);

        assertTrue(approved.isApproved());
        verify(vendorRepository, times(1)).save(sampleVendor);
    }
}
