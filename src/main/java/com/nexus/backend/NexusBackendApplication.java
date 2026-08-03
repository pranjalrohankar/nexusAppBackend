package com.nexus.backend;

import com.nexus.backend.enums.BatchStatus;
import com.nexus.backend.enums.ClassDay;
import com.nexus.backend.model.Batch;
import com.nexus.backend.model.Course;
import com.nexus.backend.model.User;
import com.nexus.backend.repository.BatchRepository;
import com.nexus.backend.repository.CourseRepository;
import com.nexus.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class NexusBackendApplication {

    private static final Logger log = LoggerFactory.getLogger(NexusBackendApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NexusBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository,
                                CourseRepository courseRepository,
                                BatchRepository batchRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                if (!userRepository.existsByEmail("admin@nexus.com")) {
                    User admin = new User();
                    admin.setName("Admin");
                    admin.setEmail("admin@nexus.com");
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setRole(User.Role.ADMIN);
                    userRepository.save(admin);
                    log.info("Admin user seeded successfully");
                }

                if (courseRepository.count() == 0) {
                    Course c1 = Course.builder()
                        .title("Full Stack Web Development")
                        .category("Development")
                        .description("Master MERN stack development from scratch.")
                        .duration("3 Months")
                        .instructor("Rajesh Kumar")
                        .classTimings("08:00 AM - 10:00 AM")
                        .classDays("Mon, Wed, Fri")
                        .googleMeetLink("https://meet.google.com/miq-hydh-kkf")
                        .price(new BigDecimal("499.00"))
                        .status(Course.Status.ACTIVE)
                        .build();

                    Course c2 = Course.builder()
                        .title("Java Full Stack Development")
                        .category("Development")
                        .description("Learn Spring Boot, Hibernate and Angular/React.")
                        .duration("3.5 Months")
                        .instructor("Amit Patel")
                        .classTimings("06:00 PM - 08:00 PM")
                        .classDays("Tue, Thu, Sat")
                        .googleMeetLink("https://meet.google.com/miq-hydh-kkf")
                        .price(new BigDecimal("599.00"))
                        .status(Course.Status.ACTIVE)
                        .build();

                    Course c3 = Course.builder()
                        .title("Data Science & Machine Learning")
                        .category("Data Science")
                        .description("Python, Pandas, ML algorithms and deep learning.")
                        .duration("4 Months")
                        .instructor("Priya Sharma")
                        .classTimings("10:00 AM - 12:00 PM")
                        .classDays("Mon, Wed, Sat")
                        .googleMeetLink("https://meet.google.com/miq-hydh-kkf")
                        .price(new BigDecimal("699.00"))
                        .status(Course.Status.ACTIVE)
                        .build();

                    Course c4 = Course.builder()
                        .title("UI/UX Design Mastery")
                        .category("Design")
                        .description("Figma, user research, wireframing and prototyping.")
                        .duration("3 Months")
                        .instructor("Karan Malhotra")
                        .classTimings("02:00 PM - 05:00 PM")
                        .classDays("Sat, Sun")
                        .googleMeetLink("https://meet.google.com/miq-hydh-kkf")
                        .price(new BigDecimal("399.00"))
                        .status(Course.Status.ACTIVE)
                        .build();

                    courseRepository.saveAll(List.of(c1, c2, c3, c4));
                    log.info("Default courses seeded successfully");
                }

                if (batchRepository.count() == 0) {
                    Batch b1 = new Batch();
                    b1.setBatchName("FSWD - Morning Batch A");
                    b1.setSelectCourse("Full Stack Web Development");
                    b1.setInstructor("Rajesh Kumar");
                    b1.setStartDate(LocalDate.of(2026, 6, 1));
                    b1.setEndDate(LocalDate.of(2026, 9, 1));
                    b1.setClassDays(List.of(ClassDay.MON, ClassDay.WED, ClassDay.FRI));
                    b1.setStatus(BatchStatus.ACTIVE);
                    b1.setClassTimings("08:00 AM - 10:00 AM");
                    b1.setDuration("3 Months");
                    b1.setGoogleMeetLink("https://meet.google.com/miq-hydh-kkf");

                    Batch b2 = new Batch();
                    b2.setBatchName("Java Full Stack - Evening Batch");
                    b2.setSelectCourse("Java Full Stack Development");
                    b2.setInstructor("Amit Patel");
                    b2.setStartDate(LocalDate.of(2026, 6, 15));
                    b2.setEndDate(LocalDate.of(2026, 10, 1));
                    b2.setClassDays(List.of(ClassDay.TUE, ClassDay.THU, ClassDay.SAT));
                    b2.setStatus(BatchStatus.ACTIVE);
                    b2.setClassTimings("06:00 PM - 08:00 PM");
                    b2.setDuration("3.5 Months");
                    b2.setGoogleMeetLink("https://meet.google.com/miq-hydh-kkf");

                    Batch b3 = new Batch();
                    b3.setBatchName("Data Science - Fast Track");
                    b3.setSelectCourse("Data Science & Machine Learning");
                    b3.setInstructor("Priya Sharma");
                    b3.setStartDate(LocalDate.of(2026, 8, 15));
                    b3.setEndDate(LocalDate.of(2026, 12, 15));
                    b3.setClassDays(List.of(ClassDay.MON, ClassDay.WED, ClassDay.SAT));
                    b3.setStatus(BatchStatus.UPCOMING);
                    b3.setClassTimings("10:00 AM - 12:00 PM");
                    b3.setDuration("4 Months");
                    b3.setGoogleMeetLink("https://meet.google.com/miq-hydh-kkf");

                    Batch b4 = new Batch();
                    b4.setBatchName("UI/UX Design - Weekend Batch");
                    b4.setSelectCourse("UI/UX Design Mastery");
                    b4.setInstructor("Karan Malhotra");
                    b4.setStartDate(LocalDate.of(2026, 5, 1));
                    b4.setEndDate(LocalDate.of(2026, 8, 1));
                    b4.setClassDays(List.of(ClassDay.SAT, ClassDay.SUN));
                    b4.setStatus(BatchStatus.ACTIVE);
                    b4.setClassTimings("02:00 PM - 05:00 PM");
                    b4.setDuration("3 Months");
                    b4.setGoogleMeetLink("https://meet.google.com/miq-hydh-kkf");

                    batchRepository.saveAll(List.of(b1, b2, b3, b4));
                    log.info("Default batches seeded successfully");
                }
            } catch (Exception e) {
                log.warn("Seed skipped: {}", e.getMessage());
            }
        };
    }
}
