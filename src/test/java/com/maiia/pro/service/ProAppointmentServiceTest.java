package com.maiia.pro.service;

import com.maiia.pro.EntityFactory;
import com.maiia.pro.entity.Appointment;
import com.maiia.pro.entity.Availability;
import com.maiia.pro.entity.Practitioner;
import com.maiia.pro.repository.AppointmentRepository;
import com.maiia.pro.repository.AvailabilityRepository;
import com.maiia.pro.repository.PractitionerRepository;
import com.maiia.pro.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProAppointmentServiceTest {

    private final EntityFactory entityFactory = new EntityFactory();
    private final static Integer patientId = 657679;

    @Autowired
    private ProAppointmentService proAppointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private PractitionerRepository practitionerRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @BeforeEach
    void setUp() {
        // Clean up repositories before each test
        appointmentRepository.deleteAll();
        availabilityRepository.deleteAll();
    }

    @Test
    void generateAppointment() {
        // Given
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));
        Appointment appointment = entityFactory.createAppointment(practitioner.getId(), patientId, startDate.plusMinutes(15), startDate.plusMinutes(30));

        availabilityRepository.save(new Availability(null, practitioner.getId(), startDate, startDate.plusHours(1)));

        // When
        proAppointmentService.generateAppointment(appointment);

        // Then
        List<Appointment> appointments = appointmentRepository.findByPractitionerId(practitioner.getId());
        assertEquals(1, appointments.size());
        assertEquals(appointment, appointments.get(0));
    }

    @Test
    void checkAppointmentsAreNotDuplicated() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));

        Appointment existingAppointment = entityFactory.createAppointment(practitioner.getId(), patientId, startDate.plusMinutes(10), startDate.plusMinutes(25));
        appointmentRepository.save(existingAppointment);

        Appointment newAppointment = entityFactory.createAppointment(practitioner.getId(), patientId, startDate.plusMinutes(20), startDate.plusMinutes(30));
        availabilityRepository.save(new Availability(null, practitioner.getId(), startDate, startDate.plusHours(1)));

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                proAppointmentService.generateAppointment(newAppointment)
        );
        assertEquals("Can't create appointment", thrown.getMessage());

        List<Appointment> appointments = appointmentRepository.findByPractitionerId(practitioner.getId());
        assertEquals(1, appointments.size());
    }

    @Test
    void noAvailabilityCantCreateAppointement() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));
        Appointment appointment = entityFactory.createAppointment(practitioner.getId(), patientId, startDate.plusMinutes(15), startDate.plusMinutes(30));

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                proAppointmentService.generateAppointment(appointment)
        );
        assertEquals("Can't create appointment", thrown.getMessage());

        List<Appointment> appointments = appointmentRepository.findByPractitionerId(practitioner.getId());
        assertEquals(0, appointments.size());
    }
}
