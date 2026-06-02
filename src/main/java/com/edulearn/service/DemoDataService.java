package com.edulearn.service;

import com.edulearn.entity.*;
import com.edulearn.enums.ContentStatus;
import com.edulearn.enums.QuestionType;
import com.edulearn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DemoDataService {

    private final JdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepo;
    private final SubjectRepository subjectRepo;
    private final TopicRepository topicRepo;
    private final QuestionRepository questionRepo;
    private final QuestionOptionRepository optionRepo;

    // ─── Static catalogue ────────────────────────────────────────────────────

    private static final String[][] CAT = {
        {"Year 7",            "Year 7 curriculum — introduction to secondary school subjects"},
        {"Year 8",            "Year 8 curriculum — building on Year 7 foundations"},
        {"Year 9",            "Year 9 curriculum — preparing students for GCSE pathways"},
        {"Year 10 (GCSE)",    "GCSE Year 1 — developing core knowledge and exam technique"},
        {"Year 11 (GCSE)",    "GCSE Year 2 — consolidation and examination preparation"},
        {"Year 12 (A-Level)", "Sixth Form Year 1 — advanced study and independent learning"},
    };

    // 2–4 subjects per category position (index matches CAT)
    private static final String[][][] SUBS = {
        {{"Mathematics","Foundation numeracy and problem-solving skills"},
         {"English Language","Core reading, writing and communication skills"}},

        {{"Mathematics","Building on Year 7 mathematics foundations"},
         {"English Language","Advanced literacy, analysis and writing"},
         {"Science","Introduction to Biology, Chemistry and Physics"}},

        {{"Mathematics","Pre-GCSE mathematics — bridging the gap"},
         {"English Language","GCSE preparation literacy and language analysis"},
         {"Geography","Physical and human geography — key concepts"}},

        {{"Mathematics","GCSE Mathematics — number, algebra, geometry and statistics"},
         {"English Language","GCSE English Language — reading and writing"},
         {"Biology","GCSE Biology — life processes and living things"},
         {"Physics","GCSE Physics — forces, energy, waves and space"}},

        {{"Mathematics","GCSE Mathematics revision and exam preparation"},
         {"English Language","GCSE English Language revision"},
         {"Chemistry","GCSE Chemistry — atomic structure, bonding and reactions"},
         {"Computer Science","GCSE Computer Science — theory and programming"}},

        {{"Mathematics","A-Level Pure and Applied Mathematics"},
         {"Physics","A-Level Physics — mechanics, fields and quantum theory"},
         {"Computer Science","A-Level Computer Science — systems and software"}},
    };

    // Up to 10 topics per subject name (subset is taken based on numTopics input)
    private static final Map<String, String[][]> TOPIC_BANK = new LinkedHashMap<>();
    static {
        TOPIC_BANK.put("Mathematics", new String[][]{
            {"Number Theory",       "Properties of integers, primes, factors, multiples and number systems"},
            {"Algebra",             "Manipulating expressions, solving equations and working with inequalities"},
            {"Geometry",            "Properties of 2D/3D shapes, angles, area, perimeter and volume"},
            {"Trigonometry",        "Sine, cosine and tangent rules applied to triangles and real problems"},
            {"Statistics",          "Data collection, graphs, averages, spread and interpretation"},
            {"Probability",         "Calculating probability of single, combined and conditional events"},
            {"Calculus",            "Differentiation and integration of polynomial and trigonometric functions"},
            {"Vectors",             "Representing magnitude and direction in two and three dimensions"},
            {"Sequences & Series",  "Arithmetic and geometric sequences, nth terms and sum formulas"},
            {"Functions",           "Domain, range, transformations, composite and inverse functions"},
        });
        TOPIC_BANK.put("English Language", new String[][]{
            {"Non-Fiction Writing",     "Analysing and producing non-fiction texts for different audiences and purposes"},
            {"Creative Writing",        "Imaginative narratives, descriptive writing and short stories"},
            {"Reading Comprehension",   "Extracting explicit and implicit meaning from unseen texts"},
            {"Language Analysis",       "Identifying and evaluating language techniques and their effects on the reader"},
            {"Transactional Writing",   "Formal letters, reports, speeches and articles for real audiences"},
            {"Media Texts",             "Analysing newspapers, advertisements and digital media"},
            {"Spoken Language",         "Features of spoken language, accent, dialect and Standard English"},
            {"Vocabulary & Grammar",    "Using precise vocabulary and accurate grammar in extended writing"},
            {"Poetry Analysis",         "Analysing structure, form, language and themes across poems"},
            {"Persuasive Writing",      "Using rhetoric, counter-argument and persuasive devices effectively"},
        });
        TOPIC_BANK.put("Science", new String[][]{
            {"Cell Structure",      "Prokaryotic and eukaryotic cells, organelles and their functions"},
            {"Atomic Structure",    "Protons, neutrons, electrons, electron configuration and the periodic table"},
            {"Forces & Motion",     "Speed, velocity, acceleration, Newton's Laws and resultant forces"},
            {"Energy",              "Energy stores, transfers, conservation, dissipation and efficiency"},
            {"Genetics",            "DNA, chromosomes, inheritance, Punnett squares and genetic variation"},
            {"Chemical Bonding",    "Ionic, covalent and metallic bonding and resulting physical properties"},
            {"Waves",               "Properties of transverse and longitudinal waves, reflection and refraction"},
            {"Ecosystems",          "Food chains, food webs, energy transfer and biodiversity"},
            {"Chemical Reactions",  "Word and symbol equations, types of reactions and energy changes"},
            {"Space Physics",       "Solar system, stars, life cycles and the expanding universe"},
        });
        TOPIC_BANK.put("Geography", new String[][]{
            {"Climate & Weather",       "Atmospheric processes, climate zones, weather systems and measurement"},
            {"Coastal Landforms",       "Erosion, transportation, deposition and coastal management strategies"},
            {"Population & Migration",  "Population distribution, demographic transition and migration patterns"},
            {"Urbanisation",            "Urban growth, challenges of rapid urbanisation and sustainable cities"},
            {"Natural Hazards",         "Earthquakes, volcanoes, tropical storms — causes, impacts and responses"},
            {"Ecosystems & Biomes",     "Biotic and abiotic factors, food webs and human impact on ecosystems"},
            {"River Landforms",         "River processes, erosion landforms, deposition and flood management"},
            {"Economic Development",    "Development indicators, the north–south divide, trade and globalisation"},
            {"Resource Management",     "Food, water and energy — demand, supply and sustainable management"},
            {"Climate Change",          "Causes, evidence, global impacts and mitigation or adaptation responses"},
        });
        TOPIC_BANK.put("Biology", new String[][]{
            {"Cell Biology",            "Cell structure, cell division, transport across membranes and stem cells"},
            {"Genetics & Inheritance",  "Mendelian inheritance, mutation, genetic disorders and genetic engineering"},
            {"Evolution",               "Natural selection, speciation, evidence for evolution and extinction"},
            {"Ecosystems",              "Biotic and abiotic factors, nutrient cycling, biodiversity and conservation"},
            {"Human Physiology",        "Structure and function of digestive, circulatory, respiratory and nervous systems"},
            {"Plants & Photosynthesis", "Photosynthesis, transpiration, plant hormones and their commercial uses"},
            {"Microbiology",            "Bacterial, viral and fungal pathogens, vaccines, antibiotics and immunity"},
            {"Homeostasis",             "Blood glucose regulation, thermoregulation and osmoregulation"},
            {"Respiration",             "Aerobic and anaerobic respiration, ATP and the role of mitochondria"},
            {"DNA & Protein Synthesis", "DNA replication, transcription, translation and recombinant DNA technology"},
        });
        TOPIC_BANK.put("Physics", new String[][]{
            {"Forces & Motion",     "Newton's Laws, momentum, equations of motion and free-body diagrams"},
            {"Energy",              "Energy stores, work done, power, efficiency and conservation of energy"},
            {"Waves",               "Wave properties, EM spectrum, sound and uses of different wave types"},
            {"Electricity",         "Current, voltage, resistance, power and series/parallel circuit calculations"},
            {"Magnetism",           "Magnetic fields, electromagnets, the motor effect and electromagnetic induction"},
            {"Particle Model",      "States of matter, changes of state, specific heat capacity and latent heat"},
            {"Atomic Structure",    "Nuclear model, isotopes, radioactive decay types and half-life calculations"},
            {"Space Physics",       "Stellar life cycles, forces in the solar system and the big bang theory"},
            {"Electromagnetism",    "Induced EMF, Lenz's Law, transformers and the national grid"},
            {"Nuclear Physics",     "Fission, fusion, nuclear equations and uses and dangers of radiation"},
        });
        TOPIC_BANK.put("Chemistry", new String[][]{
            {"Atomic Structure",        "Sub-atomic particles, electron configuration, isotopes and the periodic table"},
            {"Bonding & Structure",     "Ionic, covalent and metallic bonding and their effect on physical properties"},
            {"Quantitative Chemistry",  "Moles, relative masses, concentration and reacting mass calculations"},
            {"Chemical Changes",        "Reactivity series, electrolysis and extraction of metals"},
            {"Energy Changes",          "Exothermic and endothermic reactions, bond energies and Hess's Law"},
            {"Rates of Reaction",       "Factors affecting rate, collision theory and catalysts"},
            {"Organic Chemistry",       "Alkanes, alkenes, alcohols, carboxylic acids and addition polymers"},
            {"Chemical Analysis",       "Pure substances, mixtures, paper chromatography and flame tests"},
            {"Acids & Bases",           "The pH scale, neutralisation reactions and salt preparation methods"},
            {"Electrolysis",            "Electrolytic cells, electrode products and industrial applications"},
        });
        TOPIC_BANK.put("Computer Science", new String[][]{
            {"Algorithms",              "Algorithm design, flowcharts, pseudocode, efficiency and Big O notation"},
            {"Programming Fundamentals","Variables, data types, loops, conditions, functions and scope"},
            {"Data Representation",     "Binary, hexadecimal, ASCII, Unicode, images and sound encoding"},
            {"Computer Systems",        "CPU architecture, fetch-decode-execute cycle, memory and storage"},
            {"Networks & Security",     "Network types, protocols, the internet, threats and cybersecurity"},
            {"Databases",               "Relational databases, SQL queries, normalisation and entity relationships"},
            {"Boolean Logic",           "Logic gates, truth tables, De Morgan's Laws and simplification"},
            {"Software Development",    "SDLC models, testing strategies, debugging and documentation"},
            {"Computational Thinking",  "Decomposition, abstraction, pattern recognition and algorithm design"},
            {"Artificial Intelligence", "Machine learning, neural networks, natural language processing and ethics"},
        });
    }

    private static final String[][] DEFAULT_TOPICS = {
        {"Introduction & Key Concepts",   "Foundational vocabulary and core concepts underpinning this subject"},
        {"Core Principles",               "Essential principles and theories central to the discipline"},
        {"Practical Application",         "Applying subject knowledge to solve real-world problems"},
        {"Analysis & Evaluation",         "Critical analysis and structured evaluation using evidence"},
        {"Case Studies",                  "Real-world scenarios illustrating theory in context"},
        {"Problem Solving",               "Structured approaches to tackling complex subject-specific problems"},
        {"Extended Writing",              "Planning and producing well-structured, extended written responses"},
        {"Revision & Exam Technique",     "Consolidating key knowledge and developing effective exam strategies"},
    };

    private static final String[] DIFFICULTIES = {"Foundation", "Intermediate", "Higher"};

    // ─── Question type rotation ───────────────────────────────────────────────
    private static final QuestionType[] Q_TYPE_CYCLE = {
        QuestionType.MCQ_SINGLE,
        QuestionType.TRUE_FALSE,
        QuestionType.SHORT_ANSWER,
        QuestionType.MCQ_SINGLE,
        QuestionType.MCQ_MULTIPLE,
        QuestionType.SHORT_ANSWER,
        QuestionType.ESSAY,
        QuestionType.TRUE_FALSE,
        QuestionType.MCQ_SINGLE,
        QuestionType.SHORT_ANSWER,
    };

    // ─── MCQ_SINGLE question text ─────────────────────────────────────────────
    private static final String[] MCQ_Q = {
        "Which of the following statements about {topic} is most accurate?",
        "What is the primary focus of study within {topic}?",
        "In the context of {topic}, which approach is considered most effective?",
        "Which concept is fundamental to understanding {topic}?",
        "How is the key principle of {topic} best described?",
        "Which of the following is NOT a key feature of {topic}?",
        "When working with {topic}, which factor is most significant?",
        "What skill is most important when applying knowledge of {topic}?",
    };

    // 4 option sets — correct answer rotates across positions 0, 1, 2, 3
    private static final String[][] MCQ_OPTS = {
        {
            "Systematic analysis of core principles and their practical applications",  // pos 0 = correct
            "Memorising surface-level facts without conceptual understanding",
            "Applying unrelated techniques from a different discipline",
            "Avoiding analytical thinking in favour of unguided trial and error",
        },
        {
            "Focusing solely on memorised definitions without applying them",
            "Building understanding through structured practice and reflection",          // pos 1 = correct
            "Repeating the same method regardless of the problem or context",
            "Ignoring established frameworks and working without structure",
        },
        {
            "Skipping foundational steps to attempt advanced problems immediately",
            "Relying on a single method for every type of problem",
            "Developing conceptual clarity before applying procedural techniques",         // pos 2 = correct
            "Treating all questions as identical regardless of context or constraints",
        },
        {
            "Using intuition alone without any structured reasoning",
            "Applying methods that are not validated in this context",
            "Assuming all variables remain constant across different situations",
            "Identifying key variables and applying established principles correctly",      // pos 3 = correct
        },
    };
    // Which index within each MCQ_OPTS set is the correct answer
    private static final int[] MCQ_CORRECT_IDX = {0, 1, 2, 3};

    // ─── MCQ_MULTIPLE ─────────────────────────────────────────────────────────
    private static final String[] MCQM_Q = {
        "Which of the following statements about {topic} are correct? (Select ALL that apply)",
        "Select ALL responses that accurately describe aspects of {topic}.",
        "Which of the following are key considerations when studying {topic}? (Select ALL that apply)",
        "Which statements accurately describe features of {topic}? (Select ALL that apply)",
    };

    private static final String[][] MCQM_OPTS = {
        {
            "It requires structured reasoning and logical thinking",
            "It builds on foundational knowledge from earlier learning",
            "It can only be applied in purely theoretical, non-practical settings",
            "It contributes to skills that are useful in higher-level study",
        },
        {
            "It has clear links to both theoretical understanding and practical application",
            "It involves no form of problem-solving, analysis or evaluation",
            "It develops transferable analytical skills relevant beyond this subject",
            "Evidence and careful reasoning are central to understanding it",
        },
    };

    private static final boolean[][] MCQM_CORRECT = {
        {true, true, false, true},
        {true, false, true, true},
    };

    // ─── TRUE_FALSE ───────────────────────────────────────────────────────────
    private static final String[] TF_Q = {
        "{topic} is considered a core area of study within this subject.",
        "A thorough understanding of {topic} supports progress in related areas of the curriculum.",
        "{topic} has no real-world applications outside of academic study.",
        "Mastery of {topic} is widely regarded as essential for advanced study in this field.",
        "{topic} can be fully understood without any prior knowledge of foundational concepts.",
        "Assessment and examinations regularly test knowledge and application of {topic}.",
        "{topic} is only relevant to students studying at the highest academic level.",
        "The key principles of {topic} build upon foundational concepts established earlier in the subject.",
    };
    private static final boolean[] TF_CORRECT = {true, true, false, true, false, true, false, true};

    // ─── SHORT_ANSWER ─────────────────────────────────────────────────────────
    private static final String[] SA_Q = {
        "Define {topic} in your own words and briefly explain its significance.",
        "Describe two key concepts that are central to understanding {topic}.",
        "Explain one real-world application of {topic} and why it is important.",
        "Outline the main steps involved in approaching a problem that relates to {topic}.",
        "Compare and contrast {topic} with one other related concept in this subject.",
        "Explain why {topic} is considered an important area of study in this subject.",
        "Describe a common misconception about {topic} and explain why it is incorrect.",
        "Summarise the main ideas covered in {topic} in your own words.",
    };

    // ─── ESSAY ────────────────────────────────────────────────────────────────
    private static final String[] ESSAY_Q = {
        "Discuss the importance of {topic}, using relevant examples and evidence to support your answer.",
        "Evaluate the extent to which a thorough understanding of {topic} is essential for success in this subject.",
        "'Mastery of {topic} underpins achievement across the wider curriculum.' To what extent do you agree with this statement? Justify your answer.",
        "Analyse the key concepts within {topic} and explain their significance in real-world contexts.",
    };

    // ─── Public API ───────────────────────────────────────────────────────────

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("categories", categoryRepo.count());
        stats.put("subjects", subjectRepo.count());
        stats.put("topics", topicRepo.count());
        stats.put("questions", questionRepo.count());
        try { stats.put("exams", jdbc.queryForObject("SELECT COUNT(*) FROM exams WHERE is_deleted=false", Long.class)); } catch (Exception e) { stats.put("exams", 0L); }
        try { stats.put("classrooms", jdbc.queryForObject("SELECT COUNT(*) FROM classes", Long.class)); } catch (Exception e) { stats.put("classrooms", 0L); }
        try { stats.put("schedules", jdbc.queryForObject("SELECT COUNT(*) FROM exam_schedules", Long.class)); } catch (Exception e) { stats.put("schedules", 0L); }
        return stats;
    }

    @Transactional
    public Map<String, Integer> clearDemoData() {
        // Delete in FK-safe order (children before parents)
        String[][] deletes = {
            {"attempt_answers",  "DELETE FROM attempt_answers"},
            {"exam_attempts",    "DELETE FROM exam_attempts"},
            {"student_classes",  "DELETE FROM student_classes"},
            {"exam_schedules",   "DELETE FROM exam_schedules"},
            {"exam_questions",   "DELETE FROM exam_questions"},
            {"exams",            "DELETE FROM exams"},
            {"classrooms",       "DELETE FROM classes"},
            {"approval_log",     "DELETE FROM approval_log"},
            {"notifications",    "DELETE FROM notifications"},
            {"question_options", "DELETE FROM question_options"},
            {"questions",        "DELETE FROM questions"},
            {"topics",           "DELETE FROM topics"},
            {"subjects",         "DELETE FROM subjects"},
            {"categories",       "DELETE FROM categories"},
        };

        Map<String, Integer> result = new LinkedHashMap<>();
        for (String[] d : deletes) {
            try {
                result.put(d[0], jdbc.update(d[1]));
            } catch (Exception e) {
                result.put(d[0], 0);
            }
        }
        return result;
    }

    @Transactional
    public Map<String, Integer> generateDemoData(int numCategories, int topicsPerSubject, int questionsPerTopic) {
        User admin = currentAdmin();
        OffsetDateTime now = OffsetDateTime.now();

        int catCount = 0, subCount = 0, topicCount = 0, qCount = 0;

        int catLimit = Math.min(numCategories, CAT.length);

        for (int ci = 0; ci < catLimit; ci++) {
            Category cat = categoryRepo.save(Category.builder()
                .name(CAT[ci][0])
                .description(CAT[ci][1])
                .status(ContentStatus.APPROVED)
                .createdBy(admin).approvedBy(admin).approvedAt(now)
                .build());
            catCount++;

            for (String[] sd : SUBS[ci]) {
                Subject sub = subjectRepo.save(Subject.builder()
                    .category(cat)
                    .name(sd[0])
                    .description(sd[1])
                    .status(ContentStatus.APPROVED)
                    .createdBy(admin).approvedBy(admin).approvedAt(now)
                    .build());
                subCount++;

                String[][] bank = TOPIC_BANK.getOrDefault(sd[0], DEFAULT_TOPICS);
                int topicLimit = Math.min(topicsPerSubject, bank.length);

                for (int ti = 0; ti < topicLimit; ti++) {
                    Topic topic = topicRepo.save(Topic.builder()
                        .subject(sub)
                        .name(bank[ti][0])
                        .learningObjective(bank[ti][1])
                        .difficulty(DIFFICULTIES[ti % DIFFICULTIES.length])
                        .status(ContentStatus.APPROVED)
                        .createdBy(admin).approvedBy(admin).approvedAt(now)
                        .build());
                    topicCount++;

                    for (int qi = 0; qi < questionsPerTopic; qi++) {
                        createQuestion(topic, admin, now, qi);
                        qCount++;
                    }
                }
            }
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("categoriesCreated", catCount);
        result.put("subjectsCreated", subCount);
        result.put("topicsCreated", topicCount);
        result.put("questionsCreated", qCount);
        result.put("totalItems", catCount + subCount + topicCount + qCount);
        return result;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private User currentAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow();
    }

    private void createQuestion(Topic topic, User admin, OffsetDateTime now, int qi) {
        QuestionType type = Q_TYPE_CYCLE[qi % Q_TYPE_CYCLE.length];
        String tName = topic.getName();

        Question.QuestionBuilder qb = Question.builder()
            .topic(topic)
            .questionType(type)
            .status(ContentStatus.APPROVED)
            .createdBy(admin).approvedBy(admin).approvedAt(now);

        switch (type) {
            case MCQ_SINGLE -> {
                qb.questionText(MCQ_Q[qi % MCQ_Q.length].replace("{topic}", tName));
                qb.marks((short) 2);
                Question q = questionRepo.save(qb.build());
                saveSingleMcqOptions(q, qi);
            }
            case MCQ_MULTIPLE -> {
                int tmpl = qi % MCQM_Q.length;
                qb.questionText(MCQM_Q[tmpl].replace("{topic}", tName));
                qb.marks((short) 3);
                Question q = questionRepo.save(qb.build());
                saveMultiMcqOptions(q, tmpl);
            }
            case TRUE_FALSE -> {
                int tmpl = qi % TF_Q.length;
                qb.questionText(TF_Q[tmpl].replace("{topic}", tName));
                qb.marks((short) 1);
                qb.correctBoolean(TF_CORRECT[tmpl]);
                questionRepo.save(qb.build());
            }
            case SHORT_ANSWER -> {
                qb.questionText(SA_Q[qi % SA_Q.length].replace("{topic}", tName));
                qb.marks((short) 3);
                qb.wordLimit(100);
                qb.markingScheme("1 mark: clear definition. 1 mark: relevant example. 1 mark: explanation of significance.");
                questionRepo.save(qb.build());
            }
            case ESSAY -> {
                qb.questionText(ESSAY_Q[qi % ESSAY_Q.length].replace("{topic}", tName));
                qb.marks((short) 8);
                qb.wordLimit(400);
                qb.markingScheme("L1 (1-2 marks): Basic knowledge. L2 (3-5 marks): Clear explanation with examples. L3 (6-8 marks): Analytical discussion with evaluation.");
                questionRepo.save(qb.build());
            }
            default -> {
                qb.questionText("Describe a key concept from " + tName + " and explain its importance.");
                qb.marks((short) 2);
                questionRepo.save(qb.build());
            }
        }
    }

    private void saveSingleMcqOptions(Question q, int qi) {
        int setIdx = qi % MCQ_OPTS.length;
        String[] opts = MCQ_OPTS[setIdx];
        int correctIdx = MCQ_CORRECT_IDX[setIdx];
        for (int i = 0; i < opts.length; i++) {
            optionRepo.save(QuestionOption.builder()
                .question(q).optionText(opts[i])
                .isCorrect(i == correctIdx)
                .displayOrder((short) i)
                .build());
        }
    }

    private void saveMultiMcqOptions(Question q, int tmpl) {
        int setIdx = tmpl % MCQM_OPTS.length;
        String[] opts = MCQM_OPTS[setIdx];
        boolean[] correct = MCQM_CORRECT[setIdx];
        for (int i = 0; i < opts.length; i++) {
            optionRepo.save(QuestionOption.builder()
                .question(q).optionText(opts[i])
                .isCorrect(correct[i])
                .displayOrder((short) i)
                .build());
        }
    }
}
