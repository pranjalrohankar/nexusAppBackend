package com.nexus.backend;

import com.nexus.backend.enums.BatchStatus;
import com.nexus.backend.enums.ClassDay;
import com.nexus.backend.model.*;
import com.nexus.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class NexusBackendApplication {

    private static final Logger log = LoggerFactory.getLogger(NexusBackendApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NexusBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               TeacherRepository teacherRepository,
                               StudentRepository studentRepository,
                               EnrollmentRepository enrollmentRepository,
                               CourseRepository courseRepository,
                               BatchRepository batchRepository,
                               TestRepository testRepository,
                               ClassRecordingRepository recordingRepository,
                               StudyMaterialRepository materialRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            try {
                // Ensure upload directories exist
                Path matDir = Paths.get("uploads", "materials");
                Files.createDirectories(matDir);
                Path recDir = Paths.get("uploads", "recordings");
                Files.createDirectories(recDir);

                // 1. Seed Admin User
                User admin = userRepository.findByEmailIgnoreCase("admin@nexus.com").orElse(null);
                if (admin == null) {
                    admin = new User();
                    admin.setName("Admin");
                    admin.setEmail("admin@nexus.com");
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setRole(User.Role.ADMIN);
                    admin.setPhone("+91 98765 00000");
                    userRepository.save(admin);
                    log.info("Admin user seeded successfully");
                } else {
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setRole(User.Role.ADMIN);
                    userRepository.save(admin);
                }

                // 2. Seed Demo Teacher User & Teacher Profile
                User teacherUser = userRepository.findByEmailIgnoreCase("teacher@nexus.com").orElse(null);
                if (teacherUser == null) {
                    teacherUser = new User();
                    teacherUser.setName("Rajesh Kumar");
                    teacherUser.setEmail("teacher@nexus.com");
                    teacherUser.setPassword(passwordEncoder.encode("teacher123"));
                    teacherUser.setRole(User.Role.TEACHER);
                    teacherUser.setPhone("+91 98765 43210");
                    teacherUser = userRepository.save(teacherUser);
                    log.info("Teacher user seeded successfully");
                } else {
                    teacherUser.setPassword(passwordEncoder.encode("teacher123"));
                    teacherUser.setRole(User.Role.TEACHER);
                    teacherUser = userRepository.save(teacherUser);
                }
                if (teacherRepository.findByUser(teacherUser).isEmpty()) {
                    Teacher teacher = new Teacher();
                    teacher.setUser(teacherUser);
                    teacher.setName("Rajesh Kumar");
                    teacher.setEmail("teacher@nexus.com");
                    teacher.setPhone("+91 98765 43210");
                    teacher.setSpecialization("Full Stack Web Development");
                    teacher.setQualification("M.Tech in CS");
                    teacher.setExperience("8+ Years");
                    teacher.setEmploymentType("Full-Time");
                    teacher.setCity("Mumbai");
                    teacher.setState("Maharashtra");
                    teacher.setJoinDate("2024-01-15");
                    teacherRepository.save(teacher);
                    log.info("Teacher profile seeded successfully");
                }

                // 3. Seed Demo Student User & Student Profile
                User studentUser = userRepository.findByEmailIgnoreCase("student@nexus.com").orElse(null);
                if (studentUser == null) {
                    studentUser = new User();
                    studentUser.setName("Pratham Mali");
                    studentUser.setEmail("student@nexus.com");
                    studentUser.setPassword(passwordEncoder.encode("student123"));
                    studentUser.setRole(User.Role.STUDENT);
                    studentUser.setPhone("+91 98765 43211");
                    studentUser = userRepository.save(studentUser);
                    log.info("Student user seeded successfully");
                } else {
                    studentUser.setPassword(passwordEncoder.encode("student123"));
                    studentUser.setRole(User.Role.STUDENT);
                    studentUser = userRepository.save(studentUser);
                }
                Student student = studentRepository.findByUser(studentUser).orElse(null);
                if (student == null) {
                    student = new Student();
                    student.setUser(studentUser);
                    student.setName("Pratham Mali");
                    student.setEmail("student@nexus.com");
                    student.setPhone("+91 98765 43211");
                    student.setCourse("Full Stack Web Development");
                    student.setEnrollmentDate("2026-06-01");
                    student.setPaymentStatus("PAID");
                    student.setCity("Pune");
                    student.setState("Maharashtra");
                    student = studentRepository.save(student);
                    log.info("Student profile seeded successfully");
                }

                // Rich Syllabus Module JSON Constants
                String fswdSyllabus = "[{\"title\":\"Module 1: HTML5, CSS3 & Responsive Design\",\"topics\":[\"HTML5 Semantic Elements & Forms\",\"Modern CSS3 Flexbox & Grid Layouts\",\"Responsive Media Queries & Mobile First\",\"CSS Transitions & Keyframe Animations\"]},{\"title\":\"Module 2: JavaScript ES6+ & Async Programming\",\"topics\":[\"ES6 Syntax, Let/Const & Arrow Functions\",\"Closures, Prototypes & Lexical Scope\",\"Promises, Async/Await & Fetch API\",\"DOM Manipulation & Event Loop\"]},{\"title\":\"Module 3: React 19 & Frontend Architecture\",\"topics\":[\"Components, JSX & Props System\",\"Hooks (useState, useEffect, useMemo, useRef)\",\"State Management & React Context API\",\"React Router & Client-Side Navigation\"]},{\"title\":\"Module 4: Node.js & Express RESTful APIs\",\"topics\":[\"Node.js Architecture & Event-Driven I/O\",\"Express Routing, Middleware & Error Handling\",\"REST API Design & JWT Authentication\",\"File Uploads & Stream Handling\"]},{\"title\":\"Module 5: MongoDB & Full Stack Integration\",\"topics\":[\"NoSQL Document Design & Mongoose Schemas\",\"CRUD Operations & Aggregation Pipeline\",\"Connecting React Frontend to Express Backend\",\"Cloud Deployment & Production Best Practices\"]}]";
                String javaSyllabus = "[{\"title\":\"Module 1: Core Java & Object Oriented Programming\",\"topics\":[\"OOP Principles (Encapsulation, Inheritance, Polymorphism)\",\"Collections Framework (List, Set, Map)\",\"Exception Handling & Custom Exceptions\",\"Multithreading & Concurrency\"]},{\"title\":\"Module 2: Advanced Java & Database Connectivity (JDBC)\",\"topics\":[\"JDBC Architecture & Driver Types\",\"CRUD Operations & PreparedStatements\",\"Transaction Management & Savepoints\",\"DAO Design Pattern\"]},{\"title\":\"Module 3: Spring Framework Core & Spring Boot 3\",\"topics\":[\"Dependency Injection & Inversion of Control\",\"Spring Boot Starters & Auto-Configuration\",\"Spring Data JPA & Hibernate ORM\",\"Building RESTful Web Services with Spring MVC\"]},{\"title\":\"Module 4: Spring Security & Microservices Architecture\",\"topics\":[\"Spring Security 6 & JWT Token Authentication\",\"Role-Based Access Control (RBAC)\",\"Microservices Communication with REST Template / Feign\",\"API Gateway & Service Discovery\"]},{\"title\":\"Module 5: React Frontend Integration & Deployment\",\"topics\":[\"React Components & Axios API Integration\",\"JWT Token Storage & Auth Interceptors\",\"Dockerizing Spring Boot & PostgreSQL\",\"Production Deployment to Cloud (AWS / Render)\"]}]";
                String dataScienceSyllabus = "[{\"title\":\"Module 1: Python for Data Science & Numerical Computing\",\"topics\":[\"Python Syntax, Data Structures & Functions\",\"NumPy Arrays & Mathematical Operations\",\"Pandas DataFrames, Series & Data Wrangling\",\"Data Cleaning & Handling Missing Values\"]},{\"title\":\"Module 2: Exploratory Data Analysis & Visualization\",\"topics\":[\"Matplotlib & Seaborn Chart Types\",\"Statistical Distributions & Hypothesis Testing\",\"Correlation Analysis & Feature Selection\",\"Interactive Dashboards\"]},{\"title\":\"Module 3: Supervised Machine Learning Algorithms\",\"topics\":[\"Linear & Logistic Regression\",\"Decision Trees & Random Forests\",\"Support Vector Machines (SVM)\",\"Model Evaluation Metrics (Accuracy, Precision, Recall, F1)\"]},{\"title\":\"Module 4: Unsupervised Learning & Dimensionality Reduction\",\"topics\":[\"K-Means Clustering & Hierarchical Clustering\",\"Principal Component Analysis (PCA)\",\"Anomaly Detection & Outlier Handling\",\"Cross-Validation & Hyperparameter Tuning\"]},{\"title\":\"Module 5: Deep Learning Foundations & Model Deployment\",\"topics\":[\"Neural Network Architecture & Backpropagation\",\"TensorFlow & Keras Model Building\",\"Convolutional Neural Networks (CNN) Basics\",\"Deploying ML Models as REST APIs\"]}]";
                String uiuxSyllabus = "[{\"title\":\"Module 1: Design Thinking & UX Research\",\"topics\":[\"Design Thinking Process & Empathy Mapping\",\"User Interviews & Qualitative Surveys\",\"User Personas & Customer Journey Maps\",\"Competitive Analysis & Information Architecture\"]},{\"title\":\"Module 2: Wireframing & Low-Fidelity Prototyping\",\"topics\":[\"Low-Fidelity Sketching & Paper Wireframes\",\"Digital Wireframing in Figma\",\"Visual Hierarchy & Layout Grids\",\"User Flow Diagrams & Navigation Patterns\"]},{\"title\":\"Module 3: Advanced Figma & UI Design Systems\",\"topics\":[\"Typography Scales & Color Theory\",\"Auto Layout 5.0 & Responsive Components\",\"Design Tokens, Variables & Styles\",\"Building Scalable Design Systems\"]},{\"title\":\"Module 4: Interactive High-Fidelity Prototyping\",\"topics\":[\"Smart Animate & Micro-Interactions\",\"Component States & Interactive Variants\",\"Form Inputs & Dynamic Overlays\",\"Mobile & Web Responsive Prototypes\"]},{\"title\":\"Module 5: Usability Testing & Design Handoff\",\"topics\":[\"Conducting Moderated & Unmoderated Tests\",\"Single Ease Question & System Usability Scale\",\"Developer Handoff with Figma Dev Mode\",\"Design Portfolio & Case Study Creation\"]}]";

                // 4. Seed Courses
                if (courseRepository.count() == 0) {
                    Course c1 = Course.builder()
                        .title("Full Stack Web Development")
                        .category("Development")
                        .description("Master MERN stack development, modern JavaScript, and real-time cloud architectures.")
                        .duration("3 Months")
                        .instructor("Rajesh Kumar")
                        .classTimings("08:00 AM - 10:00 AM")
                        .classDays("Mon, Wed, Fri")
                        .googleMeetLink("https://meet.google.com/miq-hydh-kkf")
                        .price(new BigDecimal("499.00"))
                        .status(Course.Status.ACTIVE)
                        .totalSessions(36)
                        .syllabusTopics(fswdSyllabus)
                        .whatYouWillLearn("Full stack architectures\nProduction React patterns\nJWT Authentication & Security\nCI/CD & Cloud Deployment\nResponsive Web Applications")
                        .build();

                    Course c2 = Course.builder()
                        .title("Java Full Stack Development")
                        .category("Development")
                        .description("Learn Java 21, Spring Boot 3, Hibernate JPA, microservices, and React integration.")
                        .duration("3.5 Months")
                        .instructor("Amit Patel")
                        .classTimings("06:00 PM - 08:00 PM")
                        .classDays("Tue, Thu, Sat")
                        .googleMeetLink("https://meet.google.com/miq-hydh-kkf")
                        .price(new BigDecimal("599.00"))
                        .status(Course.Status.ACTIVE)
                        .totalSessions(42)
                        .syllabusTopics(javaSyllabus)
                        .whatYouWillLearn("Enterprise Java Backend\nSpring Boot 3 & Spring Data JPA\nMicroservices Architecture\nDocker & Cloud Deployment\nReact Frontend Integration")
                        .build();

                    Course c3 = Course.builder()
                        .title("Data Science & Machine Learning")
                        .category("Data Science")
                        .description("Python, Pandas, ML algorithms, data pipelines, and predictive analytics.")
                        .duration("4 Months")
                        .instructor("Priya Sharma")
                        .classTimings("10:00 AM - 12:00 PM")
                        .classDays("Mon, Wed, Sat")
                        .googleMeetLink("https://meet.google.com/miq-hydh-kkf")
                        .price(new BigDecimal("699.00"))
                        .status(Course.Status.ACTIVE)
                        .totalSessions(48)
                        .syllabusTopics(dataScienceSyllabus)
                        .whatYouWillLearn("Statistical Analysis & EDA\nSupervised & Unsupervised Machine Learning\nFeature Engineering & Pipelines\nModel Evaluation & Optimization\nDeep Learning & Neural Networks")
                        .build();

                    Course c4 = Course.builder()
                        .title("UI/UX Design Mastery")
                        .category("Design")
                        .description("Figma design systems, UX research, wireframing, interactive prototyping, and design tokens.")
                        .duration("3 Months")
                        .instructor("Karan Malhotra")
                        .classTimings("02:00 PM - 05:00 PM")
                        .classDays("Sat, Sun")
                        .googleMeetLink("https://meet.google.com/miq-hydh-kkf")
                        .price(new BigDecimal("399.00"))
                        .status(Course.Status.ACTIVE)
                        .totalSessions(24)
                        .syllabusTopics(uiuxSyllabus)
                        .whatYouWillLearn("Design Thinking & User Research\nFigma Advanced Auto Layout\nDesign Systems & Token Architecture\nInteractive High-Fidelity Prototyping\nUsability Testing & Design Handoff")
                        .build();

                    courseRepository.saveAll(List.of(c1, c2, c3, c4));
                    log.info("Default courses seeded successfully");
                } else {
                    // Update existing courses if syllabus is not yet structured JSON
                    List<Course> existing = courseRepository.findAll();
                    for (Course c : existing) {
                        String syl = c.getSyllabusTopics();
                        if (syl == null || !syl.trim().startsWith("[")) {
                            if (c.getTitle() != null && c.getTitle().contains("Full Stack Web")) {
                                c.setSyllabusTopics(fswdSyllabus);
                                courseRepository.save(c);
                            } else if (c.getTitle() != null && c.getTitle().contains("Java Full Stack")) {
                                c.setSyllabusTopics(javaSyllabus);
                                courseRepository.save(c);
                            } else if (c.getTitle() != null && c.getTitle().contains("Data Science")) {
                                c.setSyllabusTopics(dataScienceSyllabus);
                                courseRepository.save(c);
                            } else if (c.getTitle() != null && c.getTitle().contains("UI/UX")) {
                                c.setSyllabusTopics(uiuxSyllabus);
                                courseRepository.save(c);
                            }
                        }
                    }
                }

                // 5. Seed Batches
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

                // 6. Ensure Student Enrollments
                if (student != null) {
                    if (!enrollmentRepository.existsByStudentAndCourseTitle(student, "Full Stack Web Development")) {
                        Enrollment e1 = new Enrollment();
                        e1.setStudent(student);
                        e1.setCourseTitle("Full Stack Web Development");
                        e1.setEnrollmentDate("2026-06-01");
                        e1.setPaymentStatus("PAID");
                        enrollmentRepository.save(e1);
                    }
                    if (!enrollmentRepository.existsByStudentAndCourseTitle(student, "Java Full Stack Development")) {
                        Enrollment e2 = new Enrollment();
                        e2.setStudent(student);
                        e2.setCourseTitle("Java Full Stack Development");
                        e2.setEnrollmentDate("2026-06-15");
                        e2.setPaymentStatus("PAID");
                        enrollmentRepository.save(e2);
                    }
                }

                // 7. Seed Tests & Assessments
                if (testRepository.count() == 0) {
                    Test t1 = Test.builder()
                        .testName("JavaScript ES6+ Assessment")
                        .courseTitle("Full Stack Web Development")
                        .category("Full Stack Development")
                        .duration("35 mins")
                        .passScore("70%")
                        .totalMarks(100)
                        .testType("MCQ")
                        .questionsCount(5)
                        .createdByTeacherEmail("admin@nexus.com")
                        .createdByName("Rajesh Kumar")
                        .questionsJson("[{\"question\":\"Which keyword creates a block-scoped variable in ES6?\",\"options\":[\"var\",\"let\",\"const and let\",\"global\"],\"correctOption\":2},{\"question\":\"What does Promise.all() do?\",\"options\":[\"Rejects if any promise rejects\",\"Resolves first\",\"Runs synchronously\",\"Cancels all\"],\"correctOption\":0},{\"question\":\"What is the purpose of arrow functions?\",\"options\":[\"Lexical this binding\",\"New prototype\",\"Dynamic scope\",\"Slower execution\"],\"correctOption\":0},{\"question\":\"Which method creates a shallow copy of an array in ES6?\",\"options\":[\"Array.from() / Spread [...arr]\",\"arr.slice(-1)\",\"arr.copy()\",\"arr.shallow()\"],\"correctOption\":0},{\"question\":\"What does destructuring assignment do?\",\"options\":[\"Unpacks values from arrays/objects\",\"Destroys variables\",\"Compiles JS\",\"Deletes properties\"],\"correctOption\":0}]")
                        .createdAt(LocalDateTime.now())
                        .build();

                    Test t2 = Test.builder()
                        .testName("React Advanced Patterns Test")
                        .courseTitle("Full Stack Web Development")
                        .category("Full Stack Development")
                        .duration("45 mins")
                        .passScore("75%")
                        .totalMarks(100)
                        .testType("MCQ")
                        .questionsCount(5)
                        .createdByTeacherEmail("admin@nexus.com")
                        .createdByName("Rajesh Kumar")
                        .questionsJson("[{\"question\":\"What is the primary benefit of React hooks?\",\"options\":[\"Reuse stateful logic without changing hierarchy\",\"Replace JSX\",\"Faster than vanilla JS\",\"Disable re-renders\"],\"correctOption\":0},{\"question\":\"When does useEffect cleanup function run?\",\"options\":[\"Before component unmounts and before re-running effect\",\"Only on page reload\",\"Only on error\",\"Never\"],\"correctOption\":0},{\"question\":\"What is React.memo used for?\",\"options\":[\"Memoizing component render based on props\",\"Storing redux state\",\"Memoizing hooks\",\"Database caching\"],\"correctOption\":0},{\"question\":\"Which hook should be used for mutable values that don't trigger re-render?\",\"options\":[\"useRef\",\"useState\",\"useMemo\",\"useCallback\"],\"correctOption\":0},{\"question\":\"What problem does useCallback solve?\",\"options\":[\"Preserves function reference across renders\",\"Replaces Redux\",\"Executes async code\",\"Creates DOM nodes\"],\"correctOption\":0}]")
                        .createdAt(LocalDateTime.now())
                        .build();

                    Test t3 = Test.builder()
                        .testName("UI/UX Design Fundamentals")
                        .courseTitle("UI/UX Design Mastery")
                        .category("UI/UX Design")
                        .duration("30 mins")
                        .passScore("70%")
                        .totalMarks(100)
                        .testType("MCQ")
                        .questionsCount(5)
                        .createdByTeacherEmail("admin@nexus.com")
                        .createdByName("Karan Malhotra")
                        .questionsJson("[{\"question\":\"What is Fitts's Law primarily used for in UI design?\",\"options\":[\"Modeling target acquisition time based on distance & size\",\"Color contrast calculation\",\"Typography sizing\",\"CSS animation speed\"],\"correctOption\":0},{\"question\":\"What does Hick's Law state?\",\"options\":[\"Time to decide increases with number and complexity of choices\",\"Bigger buttons are always better\",\"Contrast should exceed 4.5:1\",\"Navigation must be at top\"],\"correctOption\":0},{\"question\":\"What is a wireframe?\",\"options\":[\"A basic visual guide of UI layout without full styling\",\"Final production code\",\"Color palette guide\",\"Vector logo\"],\"correctOption\":0},{\"question\":\"Which UX metric measures task completion ease?\",\"options\":[\"Single Ease Question (SEQ) / SUS\",\"Page views\",\"Bounce rate\",\"FPS\"],\"correctOption\":0},{\"question\":\"What is the purpose of user personas?\",\"options\":[\"Represent archetypal users to guide design decisions\",\"Marketing logos\",\"Sales tracking\",\"Code documentation\"],\"correctOption\":0}]")
                        .createdAt(LocalDateTime.now())
                        .build();

                    Test t4 = Test.builder()
                        .testName("Data Science Foundations")
                        .courseTitle("Data Science & Machine Learning")
                        .category("Data Science & Machine Learning")
                        .duration("40 mins")
                        .passScore("70%")
                        .totalMarks(100)
                        .testType("MCQ")
                        .questionsCount(5)
                        .createdByTeacherEmail("admin@nexus.com")
                        .createdByName("Priya Sharma")
                        .questionsJson("[{\"question\":\"Which Python library is primarily used for tabular data manipulation?\",\"options\":[\"Pandas\",\"PyTorch\",\"Flask\",\"Matplotlib\"],\"correctOption\":0},{\"question\":\"What is overfitting in machine learning?\",\"options\":[\"Model performs well on training data but poorly on test data\",\"Model underperforms everywhere\",\"Too few parameters\",\"Fast training time\"],\"correctOption\":0},{\"question\":\"Which metric evaluates classification on imbalanced datasets?\",\"options\":[\"F1-Score / ROC-AUC\",\"Accuracy\",\"Mean Squared Error\",\"R-squared\"],\"correctOption\":0},{\"question\":\"What does PCA stand for in dimensionality reduction?\",\"options\":[\"Principal Component Analysis\",\"Python Classification Algorithm\",\"Partial Component Array\",\"Predictive Correlation Analysis\"],\"correctOption\":0},{\"question\":\"Which algorithm is an ensemble of decision trees?\",\"options\":[\"Random Forest\",\"Linear Regression\",\"K-Means\",\"Naive Bayes\"],\"correctOption\":0}]")
                        .createdAt(LocalDateTime.now())
                        .build();

                    Test t5 = Test.builder()
                        .testName("Java Full Stack & Spring Boot Assessment")
                        .courseTitle("Java Full Stack Development")
                        .category("Java Full Stack")
                        .duration("45 mins")
                        .passScore("75%")
                        .totalMarks(100)
                        .testType("MCQ")
                        .questionsCount(5)
                        .createdByTeacherEmail("admin@nexus.com")
                        .createdByName("Amit Patel")
                        .questionsJson("[{\"question\":\"Which Spring annotation maps an HTTP GET request to a handler method?\",\"options\":[\"@GetMapping\",\"@PostMapping\",\"@RequestMapping(method=POST)\",\"@QueryMapping\"],\"correctOption\":0},{\"question\":\"What is Dependency Injection in Spring?\",\"options\":[\"Objects receive dependencies from external container\",\"Hardcoded object creation\",\"Java Reflection bypass\",\"Thread pooling\"],\"correctOption\":0},{\"question\":\"What is JPA used for in Spring Boot?\",\"options\":[\"Object-Relational Mapping (ORM) and data persistence\",\"Frontend routing\",\"JWT generation\",\"Load balancing\"],\"correctOption\":0},{\"question\":\"Which interface does Spring Data JPA repository extend for standard CRUD?\",\"options\":[\"JpaRepository / CrudRepository\",\"Serializable\",\"Runnable\",\"Callable\"],\"correctOption\":0},{\"question\":\"What does @Transactional ensure in Spring?\",\"options\":[\"ACID compliance across method operations\",\"Fast serialization\",\"Thread safety only\",\"Cache eviction\"],\"correctOption\":0}]")
                        .createdAt(LocalDateTime.now())
                        .build();

                    testRepository.saveAll(List.of(t1, t2, t3, t4, t5));
                    log.info("Default tests seeded successfully");
                }

                // 8. Seed Class Recordings
                if (recordingRepository.count() == 0) {
                    ClassRecording r1 = new ClassRecording();
                    r1.setTitle("Orientation & Full Stack Roadmap 2026");
                    r1.setDescription("Introduction to full stack web development architectures, toolchains, and project expectations.");
                    r1.setCourse("Full Stack Web Development");
                    r1.setBatch("FSWD - Morning Batch A");
                    r1.setClassDate(LocalDate.of(2026, 6, 2));
                    r1.setDuration("1 hr 15 mins");
                    r1.setFileName("Online Video Lecture");
                    r1.setFileUrl("https://www.youtube.com/watch?v=nu_pCVPKzTk");
                    r1.setFileType("video/mp4");
                    r1.setUploadedByEmail("teacher@nexus.com");
                    r1.setUploadedByRole("TEACHER");
                    r1.setUploadedAt(LocalDateTime.now().minusDays(3));
                    recordingRepository.save(r1);

                    ClassRecording r2 = new ClassRecording();
                    r2.setTitle("Spring Boot 3 Core Architecture & Microservices");
                    r2.setDescription("Deep dive into Spring IOC, bean lifecycle, JPA entities, and REST API design.");
                    r2.setCourse("Java Full Stack Development");
                    r2.setBatch("Java Full Stack - Evening Batch");
                    r2.setClassDate(LocalDate.of(2026, 6, 16));
                    r2.setDuration("1 hr 30 mins");
                    r2.setFileName("Online Video Lecture");
                    r2.setFileUrl("https://www.youtube.com/watch?v=35EQXmHKZYs");
                    r2.setFileType("video/mp4");
                    r2.setUploadedByEmail("teacher@nexus.com");
                    r2.setUploadedByRole("TEACHER");
                    r2.setUploadedAt(LocalDateTime.now().minusDays(1));
                    recordingRepository.save(r2);

                    log.info("Default class recordings seeded successfully with real lecture video links");
                }

                // 9. Seed Study Materials
                if (materialRepository.count() == 0) {
                    Path mat1Path = matDir.resolve("javascript_es6_notes.pdf");
                    if (!Files.exists(mat1Path)) {
                        Files.writeString(mat1Path, "JavaScript ES6+ Deep Dive and Notes\nNexus LMS");
                    }
                    StudyMaterial m1 = new StudyMaterial();
                    m1.setTitle("JavaScript ES6+ Complete Cheat Sheet & Notes");
                    m1.setDescription("Complete reference for ES6+ syntax, closures, promises, async/await, and event loops.");
                    m1.setCourse("Full Stack Web Development");
                    m1.setBatch("FSWD - Morning Batch A");
                    m1.setModuleName("Module 1: Modern JavaScript");
                    m1.setTopic("ES6 Syntax & Async Programming");
                    m1.setFileName("javascript_es6_notes.pdf");
                    m1.setFilePath("uploads/materials/javascript_es6_notes.pdf");
                    m1.setFileType("application/pdf");
                    m1.setUploadedByEmail("teacher@nexus.com");
                    m1.setUploadedByRole("TEACHER");
                    m1.setUploadedAt(LocalDateTime.now().minusDays(5));
                    StudyMaterial sm1 = materialRepository.save(m1);
                    sm1.setFileUrl("/api/materials/download/" + sm1.getId());
                    materialRepository.save(sm1);

                    Path mat2Path = matDir.resolve("react_architecture_guide.pdf");
                    if (!Files.exists(mat2Path)) {
                        Files.writeString(mat2Path, "React 19 Architecture Guide\nNexus LMS");
                    }
                    StudyMaterial m2 = new StudyMaterial();
                    m2.setTitle("React 19 & Architecture Design Guide");
                    m2.setDescription("Best practices for React components, custom hooks, context, and state management.");
                    m2.setCourse("Full Stack Web Development");
                    m2.setBatch("FSWD - Morning Batch A");
                    m2.setModuleName("Module 2: React Core & Patterns");
                    m2.setTopic("Hooks & Component Design");
                    m2.setFileName("react_architecture_guide.pdf");
                    m2.setFilePath("uploads/materials/react_architecture_guide.pdf");
                    m2.setFileType("application/pdf");
                    m2.setUploadedByEmail("teacher@nexus.com");
                    m2.setUploadedByRole("TEACHER");
                    m2.setUploadedAt(LocalDateTime.now().minusDays(2));
                    StudyMaterial sm2 = materialRepository.save(m2);
                    sm2.setFileUrl("/api/materials/download/" + sm2.getId());
                    materialRepository.save(sm2);

                    Path mat3Path = matDir.resolve("spring_boot_handbook.pdf");
                    if (!Files.exists(mat3Path)) {
                        Files.writeString(mat3Path, "Spring Boot 3 Handbook\nNexus LMS");
                    }
                    StudyMaterial m3 = new StudyMaterial();
                    m3.setTitle("Spring Boot 3 & Microservices Handbook");
                    m3.setDescription("Hands-on guide to building production-grade REST APIs, security, and Hibernate ORM.");
                    m3.setCourse("Java Full Stack Development");
                    m3.setBatch("Java Full Stack - Evening Batch");
                    m3.setModuleName("Module 1: Spring Core & JPA");
                    m3.setTopic("REST APIs & ORM Persistence");
                    m3.setFileName("spring_boot_handbook.pdf");
                    m3.setFilePath("uploads/materials/spring_boot_handbook.pdf");
                    m3.setFileType("application/pdf");
                    m3.setUploadedByEmail("admin@nexus.com");
                    m3.setUploadedByRole("ADMIN");
                    m3.setUploadedAt(LocalDateTime.now().minusDays(4));
                    StudyMaterial sm3 = materialRepository.save(m3);
                    sm3.setFileUrl("/api/materials/download/" + sm3.getId());
                    materialRepository.save(sm3);

                    log.info("Default study materials seeded successfully");
                }
            } catch (Exception e) {
                log.warn("Seed skipped: {}", e.getMessage());
            }
        };
    }
}

