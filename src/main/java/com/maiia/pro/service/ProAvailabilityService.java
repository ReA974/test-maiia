package com.maiia.pro.service;

import com.maiia.pro.entity.Appointment;
import com.maiia.pro.entity.Availability;
import com.maiia.pro.entity.TimeSlot;
import com.maiia.pro.repository.AppointmentRepository;
import com.maiia.pro.repository.AvailabilityRepository;
import com.maiia.pro.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProAvailabilityService {

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    public List<Availability> findByPractitionerId(Integer practitionerId) {
        return availabilityRepository.findByPractitionerId(practitionerId);
    }
    public List<Availability> generateAvailabilities(Integer practitionerId) {
        List<TimeSlot> timeSlots = timeSlotRepository.findByPractitionerId(practitionerId);
        List<Availability> newAvailabilities = new ArrayList<>();
        List<Appointment> appointments = appointmentRepository.findByPractitionerId(practitionerId);

        for (TimeSlot timeSlot : timeSlots) {
            LocalDateTime slotStart = timeSlot.getStartDate();
            LocalDateTime slotEnd = timeSlot.getEndDate();
            LocalDateTime currentStart = slotStart;
            int totalMinutes = (int) Duration.between(slotStart, slotEnd).toMinutes();
            int maxSlots = (totalMinutes / 15) - appointments.size();
            List<Availability> tempAvailabilities = new ArrayList<>();

            while (currentStart.isBefore(slotEnd)) {
                LocalDateTime nextStart = currentStart.plusMinutes(15);
                if (nextStart.isAfter(slotEnd)) {
                    nextStart = slotEnd;
                }
                final LocalDateTime finalCurrentStart = currentStart;
                final LocalDateTime finalNextStart = nextStart;
                boolean isOccupied = appointments.stream().anyMatch(appointment ->
                        appointment.getStartDate().isBefore(finalNextStart) &&
                                appointment.getEndDate().isAfter(finalCurrentStart)
                );

                if (isOccupied) {
                    for (Appointment appointment : appointments) {
                        if (appointment.getStartDate().isBefore(finalNextStart) &&
                                appointment.getEndDate().isAfter(finalCurrentStart)) {

                            if (appointment.getStartDate().isAfter(finalCurrentStart)) {
                                LocalDateTime availStart = finalCurrentStart;
                                LocalDateTime availEnd = appointment.getStartDate();
                                if (availEnd.isAfter(availStart)) {
                                    tempAvailabilities.add(Availability.builder()
                                            .practitionerId(practitionerId)
                                            .startDate(availStart)
                                            .endDate(availEnd)
                                            .build());
                                }
                            }

                            LocalDateTime appointmentEnd = appointment.getEndDate();
                            if (appointmentEnd.isBefore(finalNextStart)) {
                                LocalDateTime availStart = appointmentEnd;
                                LocalDateTime availEnd = finalNextStart;
                                if (availEnd.isAfter(availStart)) {
                                    tempAvailabilities.add(Availability.builder()
                                            .practitionerId(practitionerId)
                                            .startDate(availStart)
                                            .endDate(availEnd)
                                            .build());
                                }
                            }

                            currentStart = appointmentEnd;
                            break;
                        }
                    }
                } else {
                    tempAvailabilities.add(Availability.builder()
                            .practitionerId(practitionerId)
                            .startDate(finalCurrentStart)
                            .endDate(finalNextStart)
                            .build());
                    currentStart = finalNextStart;
                }
            }

            if (currentStart.isBefore(slotEnd) && currentStart.plusMinutes(15).isAfter(slotEnd)) {
                tempAvailabilities.add(Availability.builder()
                        .practitionerId(practitionerId)
                        .startDate(currentStart)
                        .endDate(slotEnd)
                        .build());
            }

            List<Availability> finalAvailabilities = tempAvailabilities.stream()
                    .filter(Availability.class::isInstance) // Ensure type safety
                    .map(a -> (Availability) a) // Cast to Availability
                    .sorted(Comparator.comparingLong(a -> Duration.between(((Availability) a).getStartDate(), ((Availability) a).getEndDate()).toMinutes()).reversed()) // Sort by duration (longest first)
                    .limit(maxSlots) // Limit to maxSlots
                    .collect(Collectors.toList());

            newAvailabilities.addAll(finalAvailabilities);
        }

        availabilityRepository.deleteAll(availabilityRepository.findByPractitionerId(practitionerId));
        if (!newAvailabilities.isEmpty()) {
            availabilityRepository.saveAll(newAvailabilities);
        }

        return availabilityRepository.findByPractitionerId(practitionerId);
    }


}
