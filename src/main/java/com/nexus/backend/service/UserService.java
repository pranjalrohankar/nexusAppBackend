package com.nexus.backend.service;

import com.nexus.backend.dto.CreateUserRequest;
import com.nexus.backend.model.Enrollment;
import com.nexus.backend.model.Student;
import com.nexus.backend.model.Teacher;
import com.nexus.backend.model.User;
import com.nexus.backend.repository.EnrollmentRepository;
import com.nexus.backend.repository.StudentRepository;
import com.nexus.backend.repository.TeacherRepository;
import com.nexus.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public User createUser(CreateUserRequest req) {
        User.Role role = User.Role.valueOf(req.getRole().toUpperCase());
        User existing = userRepository.findByEmail(req.getEmail()).orElse(null);
        boolean isNew = (existing == null);
        String rawPassword = req.getPassword();

        User user;
        if (isNew) {
            if (rawPassword == null || rawPassword.isBlank()) {
                throw new RuntimeException("Password is required for new registration.");
            }
            user = new User();
            user.setName(req.getFirstName() + " " + req.getLastName());
            user.setEmail(req.getEmail());
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setPhone(req.getPhone());
            user.setRole(role);
            user = userRepository.save(user);

            if (role == User.Role.STUDENT) {
                saveStudentProfile(user, req);
            } else if (role == User.Role.TEACHER) {
                saveTeacherProfile(user, req);
            }
        } else {
            if (existing == null) {
                throw new RuntimeException("User not found");
            }
            user = existing;
            // Re-enrollment: update password to whatever admin entered
            // so the student can login with the password shown in the email
            if (rawPassword == null || rawPassword.isBlank()) {
                throw new RuntimeException("Password is required to re-enroll student.");
            }
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);

            if (role == User.Role.STUDENT && req.getCourse() != null && !req.getCourse().isBlank()) {
                Student s = studentRepository.findByUser(user).orElse(new Student());
                s.setUser(user);
                if (s.getCourse() == null) s.setCourse(req.getCourse());
                s.setEnrollmentDate(req.getEnrollmentDate());
                s.setPaymentStatus(req.getPaymentStatus());
                studentRepository.save(s);
                // Add enrollment row only if not already enrolled in this course
                if (!enrollmentRepository.existsByStudentAndCourseTitle(s, req.getCourse())) {
                    Enrollment e = new Enrollment();
                    e.setStudent(s);
                    e.setCourseTitle(req.getCourse());
                    e.setEnrollmentDate(req.getEnrollmentDate());
                    e.setPaymentStatus(req.getPaymentStatus());
                    enrollmentRepository.save(e);
                } else {
                    throw new RuntimeException("Student is already enrolled in this course.");
                }
            }
        }

        try {
            if (role == User.Role.TEACHER) {
                Teacher t = teacherRepository.findByUser(user).orElse(null);
                String spec = t != null ? t.getSpecialization() : "";
                String emp  = t != null ? t.getEmploymentType() : "";
                emailService.sendCredentials(user.getEmail(), user.getName(), "TEACHER", rawPassword, spec + "|" + emp, isNew);
            } else {
                String course = req.getCourse() != null ? req.getCourse() : "";
                emailService.sendCredentials(user.getEmail(), user.getName(), "STUDENT", rawPassword, course, isNew);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Email failed: " + e.getMessage());
        }

        return user;
    }

    private void saveStudentProfile(User user, CreateUserRequest req) {
        Student s = new Student();
        s.setUser(user);
        s.setName(user.getName());
        s.setEmail(user.getEmail());
        s.setPhone(user.getPhone());
        s.setDob(req.getDob());
        s.setStreet(req.getStreet());
        s.setCity(req.getCity());
        s.setState(req.getState());
        s.setPinCode(req.getPinCode());
        s.setGuardianName(req.getGuardianName());
        s.setGuardianPhone(req.getGuardianPhone());
        s.setCourse(req.getCourse());
        s.setEnrollmentDate(req.getEnrollmentDate());
        s.setPaymentStatus(req.getPaymentStatus());
        System.out.println(">>> Saving Student: name=" + user.getName() + ", dob=" + req.getDob()
            + ", city=" + req.getCity() + ", course=" + req.getCourse()
            + ", guardian=" + req.getGuardianName() + ", pinCode=" + req.getPinCode()
            + ", enrollmentDate=" + req.getEnrollmentDate() + ", paymentStatus=" + req.getPaymentStatus());
        studentRepository.save(s);

        // Create first enrollment record
        if (req.getCourse() != null && !req.getCourse().isBlank()) {
            Enrollment e = new Enrollment();
            e.setStudent(s);
            e.setCourseTitle(req.getCourse());
            e.setEnrollmentDate(req.getEnrollmentDate());
            e.setPaymentStatus(req.getPaymentStatus());
            enrollmentRepository.save(e);
        }
    }

    private void saveTeacherProfile(User user, CreateUserRequest req) {
        Teacher t = new Teacher();
        t.setUser(user);
        t.setName(user.getName());
        t.setEmail(user.getEmail());
        t.setPhone(user.getPhone());
        t.setDob(req.getDob());
        t.setStreet(req.getStreet());
        t.setCity(req.getCity());
        t.setState(req.getState());
        t.setPinCode(req.getPinCode());
        t.setQualification(req.getQualification());
        t.setExperience(req.getExperience());
        t.setSpecialization(req.getSpecialization());
        t.setJoinDate(req.getJoinDate());
        t.setEmploymentType(req.getEmploymentType());
        System.out.println(">>> Saving Teacher: name=" + user.getName() + ", qual=" + req.getQualification()
            + ", spec=" + req.getSpecialization() + ", exp=" + req.getExperience()
            + ", joinDate=" + req.getJoinDate() + ", empType=" + req.getEmploymentType()
            + ", city=" + req.getCity() + ", pinCode=" + req.getPinCode());
        teacherRepository.save(t);
    }

    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(User.Role.valueOf(role.toUpperCase()));
    }
}
