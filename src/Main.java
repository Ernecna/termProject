import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    ////////////////////////////////////////////////////////////////// READ&DİSPLAY CLASSROOM
    private static List<Classroom> readClassroomsFromCSV(String fileName) {
        List<Classroom> classrooms = new ArrayList<>();
        String line = "";
        String splitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader("Classes.csv"))) {
            while ((line = br.readLine()) != null) {
                String[] classroomData = line.split(splitBy);
                Classroom classroom = new Classroom(classroomData[0], Integer.parseInt(classroomData[1]));
                classrooms.add(classroom);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classrooms;
    }
    public static void displayClasses(List<Classroom> classrooms){
        for (Classroom classroom : classrooms) {
            System.out.println("Room ID: " + classroom.getRoomID() + ", Capacity: " + classroom.getCapacity());
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////////READ&&DİSPLAY CLASSLİST
    private static List<ClassList> readClassListFromCSV(String fileName) {
        List<ClassList> classLists = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] classData = line.split(",");
                ClassList classList = new ClassList(Integer.parseInt(classData[0].trim()),
                        classData[1].trim(),
                        classData[2].trim(),
                        Integer.parseInt(classData[3].trim()));
                classLists.add(classList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classLists;
    }
    public static void displayClassList(List<ClassList> classlist){
        for (ClassList classList : classlist) {
            System.out.println("Student ID: " + classList.getStudentID() +
                    ", Professor Name: " + classList.getProfessorName() +
                    ", Course ID: " + classList.getCourseID() +
                    ", Exam Duration: " + classList.getExamDuration());
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////CREATE GRAPH BY USİNG EXAM NODE
    private static AllExam createExamGraph(List<ClassList> classLists) {
        AllExam allExam = new AllExam();
        Map<String, Exam> courseExamMap = new HashMap<>();

        // Group students by course ID
        for (ClassList classList : classLists) {
            String courseID = classList.getCourseID();

            if (!courseExamMap.containsKey(courseID)) {
                courseExamMap.put(courseID, new Exam(new ArrayList<>(),
                        classList.getProfessorName(),
                        classList.getCourseID(),
                        classList.getExamDuration()));
            }
            courseExamMap.get(courseID).getStudentIDs().add(classList.getStudentID());
        }

        // Add nodes to the graph
        for (Exam exam : courseExamMap.values()) {
            allExam.addNode(exam);
        }

        return allExam;
    }




    //////////////////////////////////////////  ALGO  //////////////////////////////////////////////////////////////
    private static final int DAYS = 6; // Monday to Saturday
    private static final int HOURS = 9; // 09:00 to 18:00
    private static final String[] DAY_NAMES = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

    // A class to represent a list of exams in a single slot
    private static class ExamSlot {
        List<Exam> exams = new ArrayList<>();

        public void addExam(Exam exam) {
            exams.add(exam);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Exam exam : exams) {
                sb.append(exam.getCourseID()).append(" (").append(exam.getProfessorName()).append("), ");
            }
            return sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "Free";
        }
    }

    // Method to create and initialize the timetable
    private static ExamSlot[][] createTimetable() {
        return new ExamSlot[DAYS][HOURS];
    }

    // Method to schedule exams in the timetable
    private static void scheduleExams(AllExam allExam, ExamSlot[][] timetable) {
        Random rand = new Random();
        Map<String, Set<Integer>> professorDaysMap = new HashMap<>();

        for (Exam exam : allExam.getNodes()) {
            boolean scheduled = false;
            while (!scheduled) {
                int day = rand.nextInt(DAYS);
                int hour = rand.nextInt(HOURS);

                if (canScheduleExam(timetable, day, hour, exam.getProfessorName(), professorDaysMap)) {
                    if (timetable[day][hour] == null) {
                        timetable[day][hour] = new ExamSlot();
                    }
                    timetable[day][hour].addExam(exam);
                    scheduled = true;
                    professorDaysMap.computeIfAbsent(exam.getProfessorName(), k -> new HashSet<>()).add(day);
                }
            }
        }
    }

    // Method to check if an exam can be scheduled at the given day and hour
    // Method to check if an exam can be scheduled at the given day and hour
    private static boolean canScheduleExam(ExamSlot[][] timetable, int day, int hour, String professorName, Map<String, Set<Integer>> professorDaysMap) {
        if (timetable[day][hour] != null) {
            for (Exam exam : timetable[day][hour].exams) {
                if (exam.getProfessorName().equals(professorName)) {
                    return false; // Professor already has an exam at this slot
                }
            }
        }

        // Check if the professor already has an exam on this day
        return !professorDaysMap.getOrDefault(professorName, Collections.emptySet()).contains(day);
    }

    private static void printTimetable(ExamSlot[][] timetable) {
        for (int day = 0; day < DAYS; day++) {
            System.out.println(DAY_NAMES[day] + ":");
            for (int hour = 0; hour < HOURS; hour++) {
                System.out.print("  " + (hour + 9) + ":00 - ");
                if (timetable[day][hour] == null || timetable[day][hour].exams.isEmpty()) {
                    System.out.println("Free");
                } else {
                    for (Exam exam : timetable[day][hour].exams) {
                        System.out.print(exam.getCourseID() + " (" + exam.getProfessorName() + ") ");
                    }
                    System.out.println();
                }
            }
            System.out.println();
        }
    }



    /////////////////////////////////  CALCULATE FAULT SCORE  /////////////////////////////////////////////
    private static String createTimeSlotKey(int day, int hour) {
        return day + "-" + hour;
    }

    private static int calculateFaultScore(ExamSlot[][] timetable) {
        Map<Integer, Map<String, Integer>> studentExamTimings = new HashMap<>();
        int faultScore = 0;

        // Populate the map with exam timings for each student
        for (int day = 0; day < DAYS; day++) {
            for (int hour = 0; hour < HOURS; hour++) {
                if (timetable[day][hour] != null && !timetable[day][hour].exams.isEmpty()) {
                    String timeSlotKey = createTimeSlotKey(day, hour);
                    for (Exam exam : timetable[day][hour].exams) {
                        for (Integer studentID : exam.getStudentIDs()) {
                            studentExamTimings.computeIfAbsent(studentID, k -> new HashMap<>())
                                    .merge(timeSlotKey, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        // Count conflicts for each student in each time slot
        for (Map<String, Integer> timings : studentExamTimings.values()) {
            for (int count : timings.values()) {
                if (count > 1) {
                    faultScore += count - 1; // For each overlapping exam in a time slot
                }
            }
        }

        return faultScore;
    }


    ///////////////////////////////////////////  SIMULATED ANNEALING  //////////////////////////////7
    private static boolean simulatedAnnealing(ExamSlot[][] timetable) {
        Random rand = new Random();
        int currentFaultScore = calculateFaultScore(timetable);
        int iteration = 0;

        while (currentFaultScore > 0) {
            iteration++;

            // 1. Randomly select an exam
            int day = rand.nextInt(DAYS);
            int hour = rand.nextInt(HOURS);
            if (timetable[day][hour] == null || timetable[day][hour].exams.isEmpty()) {
                continue; // Skip if the slot is empty
            }
            Exam selectedExam = selectRandomExam(timetable[day][hour]);

            // 2. Find a new time slot
            int newDay = rand.nextInt(DAYS);
            int newHour = rand.nextInt(HOURS);

            // 3. Check if the move is valid
            if (!isValidMove(timetable, newDay, selectedExam)) {
                continue; // Skip if the move is not valid
            }

            // 4. Move the exam and calculate the new fault score
            moveExam(timetable, day, hour, newDay, newHour, selectedExam);
            int newFaultScore = calculateFaultScore(timetable);

            // 5. Decision Making
            if (newFaultScore < currentFaultScore) {
                currentFaultScore = newFaultScore; // Accept the move
            } else {
                moveExam(timetable, newDay, newHour, day, hour, selectedExam); // Revert the move
            }

            // Add termination condition or maximum iterations if needed
            if (iteration % 10000 == 0){
                System.out.println("Iteration: " + iteration + "Fault Score: " + currentFaultScore);
            }

        }


        return currentFaultScore == 0;
    }


    private static Exam selectRandomExam(ExamSlot examSlot) {
        Random rand = new Random();
        int examIndex = rand.nextInt(examSlot.exams.size());
        return examSlot.exams.get(examIndex);
    }



    private static boolean isValidMove(ExamSlot[][] timetable, int newDay, Exam exam) {
        String professorName = exam.getProfessorName();
        // Check all slots on the new day
        for (int hour = 0; hour < HOURS; hour++) {
            if (timetable[newDay][hour] != null) {
                for (Exam scheduledExam : timetable[newDay][hour].exams) {
                    if (scheduledExam.getProfessorName().equals(professorName)) {
                        return false; // Professor has an exam on the new day
                    }
                }
            }
        }
        return true; // No conflict found
    }


    private static void moveExam(ExamSlot[][] timetable, int fromDay, int fromHour, int toDay, int toHour, Exam exam) {
        // Remove exam from current slot
        if (timetable[fromDay][fromHour] != null) {
            timetable[fromDay][fromHour].exams.remove(exam);
        }

        // Add exam to new slot
        if (timetable[toDay][toHour] == null) {
            timetable[toDay][toHour] = new ExamSlot();
        }
        timetable[toDay][toHour].addExam(exam);
    }




    public static void main(String[] args) {
        List<Classroom> classrooms = readClassroomsFromCSV("Classes.csv");
        //displayClasses(classrooms);
        List<ClassList> classLists = readClassListFromCSV("1000student.csv");
        //displayClassList(classLists);

        //GRAPH READY

        AllExam allExam =createExamGraph(classLists);
        allExam.printGraph();
        System.out.println(allExam.getNumberOfNodes());

        /////////////////////////////////////////////////////////////////////////////////

        ExamSlot[][] timetable = createTimetable();
        scheduleExams(allExam, timetable);
        int initialFaultScore = calculateFaultScore(timetable);
        System.out.println("Initial Fault Score: " + initialFaultScore);
        printTimetable(timetable);

        // Simulated Annealing to resolve conflicts
        boolean isResolved = simulatedAnnealing(timetable);

        // Print the result
        System.out.println("\n\nConflict Resolution " + (isResolved ? "Successful" + "\n+++++++++++++++++++++++++++++++++++++++++++++" :
                "Failed" + "\n+++++++++++++++++++++++++++++++++++++++"));
        //System.out.println("Final Fault Score: " + finalFaultScore);

        // Print the final timetable
        printTimetable(timetable);


        // After scheduling exams
        int faultScore = calculateFaultScore(timetable);
        System.out.println("Total Fault Score (Student Conflicts): " + faultScore);




    }

}
