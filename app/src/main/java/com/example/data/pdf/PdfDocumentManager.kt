package com.example.data.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfDocumentManager {

    suspend fun getOrCreatePdf(context: Context, courseId: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "course_handbooks")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "${courseId}_handbook.pdf")
        if (file.exists() && file.length() > 1024) {
            return@withContext file
        }

        generateHandbookPdf(file, courseId)
        file
    }

    private fun generateHandbookPdf(outputFile: File, courseId: String) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val title: String
        val subtitle: String
        val sections: List<PdfSection>

        when (courseId) {
            "c" -> {
                title = "The C Programming Language"
                subtitle = "Comprehensive Architecture, Low-Level Memory & Question Bank"
                sections = getCSections()
            }
            "cpp" -> {
                title = "C++ Object-Oriented Engineering"
                subtitle = "Units 1-6: OOP, Classes, Constructors, Inheritance, Polymorphism & Streams"
                sections = getCppSections()
            }
            "java" -> {
                title = "Java Enterprise & JVM Systems"
                subtitle = "Object-Oriented Architecture, JVM Memory Model & Concurrency"
                sections = getJavaSections()
            }
            "android" -> {
                title = "Modern Android Engineering"
                subtitle = "Jetpack Compose, Declarative UI, Kotlin Coroutines & Room Architecture"
                sections = getAndroidSections()
            }
            else -> {
                title = "DevLearn Core Reference"
                subtitle = "Official Engineering Study Guide & Practice Reference"
                sections = getCSections()
            }
        }

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(71, 85, 105) // Slate 600
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val subheaderPaint = Paint().apply {
            color = Color.rgb(37, 99, 235) // Deep Blue
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85) // Slate 700
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val codePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }

        val pageNumberPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boxBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249) // Slate 100
            style = Paint.Style.FILL
        }

        val boxBorderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val codeBoxPaint = Paint().apply {
            color = Color.rgb(248, 250, 252) // Slate 50
            style = Paint.Style.FILL
        }

        // Render each section onto its own clean, structured page
        sections.forEachIndexed { pageIndex, section ->
            val pageNumber = pageIndex + 1
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Top decorative rule & header band
            canvas.drawColor(Color.WHITE)
            canvas.drawRect(36f, 36f, (pageWidth - 36).toFloat(), 38f, subheaderPaint)

            var y = 60f

            // Document branding
            canvas.drawText("DEVLEARN OFFICIAL COMPANION TEXTBOOK", 36f, y, subheaderPaint)
            y += 18f
            canvas.drawText(title, 36f, y, titlePaint)
            y += 14f
            canvas.drawText(subtitle, 36f, y, subtitlePaint)
            y += 24f

            // Section Chapter Header Box
            val headerBoxRect = RectF(36f, y, (pageWidth - 36).toFloat(), y + 36f)
            canvas.drawRoundRect(headerBoxRect, 6f, 6f, boxBgPaint)
            canvas.drawRoundRect(headerBoxRect, 6f, 6f, boxBorderPaint)
            canvas.drawText(section.heading, 48f, y + 23f, headerPaint)
            y += 52f

            // Section Description & Core Body Paragraphs
            for (paragraph in section.bodyParagraphs) {
                val lines = breakTextIntoLines(paragraph, bodyPaint, (pageWidth - 72).toFloat())
                for (line in lines) {
                    if (y > pageHeight - 70) break
                    canvas.drawText(line, 36f, y, bodyPaint)
                    y += 14f
                }
                y += 6f
            }

            // Key Technical Concepts or Points
            if (section.bulletPoints.isNotEmpty() && y < pageHeight - 160) {
                y += 4f
                canvas.drawText("Key Architecture & Concepts:", 36f, y, subheaderPaint)
                y += 16f

                for (bullet in section.bulletPoints) {
                    if (y > pageHeight - 120) break
                    canvas.drawCircle(44f, y - 3f, 2.5f, subheaderPaint)
                    val bulletLines = breakTextIntoLines(bullet, bodyPaint, (pageWidth - 92).toFloat())
                    for (bLine in bulletLines) {
                        canvas.drawText(bLine, 54f, y, bodyPaint)
                        y += 13f
                    }
                    y += 3f
                }
                y += 8f
            }

            // Code snippet box if present
            if (section.codeSnippet.isNotBlank() && y < pageHeight - 140) {
                val codeLines = section.codeSnippet.lines()
                val codeBoxHeight = (codeLines.size * 12f + 20f).coerceAtMost((pageHeight - 60) - y)

                val codeBox = RectF(36f, y, (pageWidth - 36).toFloat(), y + codeBoxHeight)
                canvas.drawRoundRect(codeBox, 4f, 4f, codeBoxPaint)
                canvas.drawRoundRect(codeBox, 4f, 4f, boxBorderPaint)

                var codeY = y + 16f
                for (cLine in codeLines) {
                    if (codeY > y + codeBoxHeight - 8f) break
                    canvas.drawText(cLine, 46f, codeY, codePaint)
                    codeY += 12f
                }
                y += codeBoxHeight + 16f
            }

            // Footer with Page Number
            val footerLineY = (pageHeight - 44).toFloat()
            canvas.drawLine(36f, footerLineY, (pageWidth - 36).toFloat(), footerLineY, boxBorderPaint)
            canvas.drawText("DevLearn Curriculum Series • $title", 36f, footerLineY + 16f, pageNumberPaint)
            val pageNumText = "Page $pageNumber of ${sections.size}"
            val pageNumWidth = pageNumberPaint.measureText(pageNumText)
            canvas.drawText(pageNumText, (pageWidth - 36).toFloat() - pageNumWidth, footerLineY + 16f, pageNumberPaint)

            document.finishPage(page)
        }

        FileOutputStream(outputFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()
    }

    private fun breakTextIntoLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = StringBuilder(candidate)
            } else {
                if (currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }
        return result
    }

    private data class PdfSection(
        val heading: String,
        val bodyParagraphs: List<String>,
        val bulletPoints: List<String> = emptyList(),
        val codeSnippet: String = ""
    )

    private fun getCSections(): List<PdfSection> = listOf(
        PdfSection(
            heading = "Unit 1: The C Compilation Model & Low-Level Memory Architecture",
            bodyParagraphs = listOf(
                "C is a compiled, imperative, procedural language developed by Dennis Ritchie at Bell Laboratories in 1972. It provides direct hardware manipulation while maintaining structured procedural abstractions.",
                "The four-stage compilation process transforms source code into executable binary: 1. Preprocessor (expands #include, macros, strips comments); 2. Compiler (translates preprocessed code into target assembly .s); 3. Assembler (translates assembly into object code .o); 4. Linker (combines object files and resolves standard C runtime symbols like printf into a single executable)."
            ),
            bulletPoints = listOf(
                "Entry point convention: int main(int argc, char *argv[]) transfers exit status to OS.",
                "Storage classes: auto (local stack), register (CPU register hint), static (data segment lifetime), extern (global linkage).",
                "Formatted I/O: printf() returns number of characters printed; scanf() returns successfully parsed items."
            ),
            codeSnippet = "#include <stdio.h>\n\nint main(void) {\n    printf(\"DevLearn C Systems Kernel Initialized\\n\");\n    int status = 0;\n    return status;\n}"
        ),
        PdfSection(
            heading = "Unit 2: Pointer Mechanics, Addresses & Indirection",
            bodyParagraphs = listOf(
                "A pointer is a variable holding the raw memory address of another entity in the process address space. In 64-bit architectures, pointers occupy 8 bytes regardless of the underlying data type they point to.",
                "Dereferencing via indirection operator (*) accesses the memory cell. The address-of operator (&) extracts the lvalue physical address.",
                "Pointer arithmetic is strictly scaled: adding 1 to an int* advances the address by sizeof(int) (typically 4 bytes), ensuring seamless traversal over contiguous array buffers."
            ),
            bulletPoints = listOf(
                "Pointer to pointer (**ptr): Holds the address of another pointer variable.",
                "Pointer to const (const int *p): Value pointed to cannot be altered via p.",
                "Constant pointer (int * const p): Address stored in p cannot be changed.",
                "Wild pointer: Uninitialized pointer pointing to arbitrary memory; Dangling pointer: points to deallocated heap."
            ),
            codeSnippet = "int value = 42;\nint *ptr = &value;       // ptr stores address of value\nint **dptr = &ptr;      // dptr points to ptr\n*ptr = 100;             // value is now 100\nprintf(\"Value: %d, Addr: %p\\n\", *ptr, (void*)ptr);"
        ),
        PdfSection(
            heading = "Unit 3: Dynamic Memory Allocation & Heap Management",
            bodyParagraphs = listOf(
                "Standard stack memory is statically allocated with lifetime scoped to function execution. The Heap provides runtime dynamic allocation controlled via <stdlib.h>.",
                "malloc(size) allocates contiguous uninitialized bytes; calloc(n, size) allocates and zeroes all memory cells. realloc(ptr, new_size) resizes an existing block. Always verify against NULL before dereferencing."
            ),
            bulletPoints = listOf(
                "Memory Leak: Failing to call free(ptr) when dynamic memory is no longer reachable.",
                "Heap fragmentation: Repetitive allocation and deallocation of varied block sizes.",
                "Rule: Every successful malloc/calloc call must correspond to exactly one free() call."
            ),
            codeSnippet = "int *arr = (int*)malloc(5 * sizeof(int));\nif (arr == NULL) return -1; // Allocation guard\nfor (int i = 0; i < 5; i++) arr[i] = (i + 1) * 10;\nfree(arr); // Prevent memory leak\narr = NULL; // Prevent dangling reference"
        ),
        PdfSection(
            heading = "Unit 4: Structures, Unions & System Files",
            bodyParagraphs = listOf(
                "Structures (struct) group heterogenous variables into a single contiguous record. Unions share the same physical memory space across all declared members, with size equal to the largest member.",
                "File operations rely on FILE* streams: fopen() with modes ('r', 'w', 'a', 'r+', 'wb'). Functions fprintf(), fscanf(), fread(), fwrite() facilitate buffered file I/O with fclose() flushing buffers."
            ),
            bulletPoints = listOf(
                "Dot operator (s.name) for direct struct instances; Arrow operator (sPtr->name) for pointer dereferences.",
                "Self-referential structures form the foundational basis for linked lists, binary trees, and graphs.",
                "Command line arguments: argc counts tokens; argv[] holds null-terminated argument strings."
            ),
            codeSnippet = "typedef struct Student {\n    int id;\n    char name[32];\n    float gpa;\n} Student;\n\nStudent s1 = {101, \"Alice\", 3.92f};\nStudent *p = &s1;\nprintf(\"ID: %d, GPA: %.2f\\n\", p->id, p->gpa);"
        )
    )

    private fun getCppSections(): List<PdfSection> = listOf(
        PdfSection(
            heading = "Unit 1: Object-Oriented Principles & POP vs OOP",
            bodyParagraphs = listOf(
                "Procedure-Oriented Programming (POP) emphasizes algorithms and functions operating on global or passed data. Object-Oriented Programming (OOP) binds data and the methods operating on that data together into cohesive objects.",
                "The core pillars of OOP are: Encapsulation (data hiding via private access), Abstraction (exposing only essential interfaces), Inheritance (code reuse and hierarchical modeling), and Polymorphism (one interface, multiple behaviors)."
            ),
            bulletPoints = listOf(
                "Encapsulation: Bundling data and member functions inside a class structure.",
                "Data Abstraction: Hiding implementation details using public/private boundaries.",
                "Manipulators: std::endl (inserts newline and flushes stream), std::setw, std::setfill."
            ),
            codeSnippet = "#include <iostream>\n#include <iomanip>\n\nint main() {\n    std::cout << \"C++ Object Architecture\" << std::endl;\n    std::cout << std::setw(10) << std::setfill('*') << 42 << std::endl;\n    return 0;\n}"
        ),
        PdfSection(
            heading = "Unit 2: Classes, Objects, Friend Functions & Member Access",
            bodyParagraphs = listOf(
                "A class in C++ serves as a user-defined blueprint. Objects are instances instantiated at runtime. Access specifiers (public, private, protected) enforce visibility boundaries.",
                "Static data members are shared across all instances of a class and must be defined outside the class scope. Static member functions can only access static data members and cannot use the 'this' pointer.",
                "A friend function or friend class is granted access to private and protected members of another class without being a member itself."
            ),
            bulletPoints = listOf(
                "Inline functions: Compiler hint to replace function calls directly with the function body to reduce call overhead.",
                "Pass by Reference (type &ref): Eliminates object copying overhead without explicit pointer syntax.",
                "Nesting of member functions: Calling one member function from inside another of the same class."
            ),
            codeSnippet = "class Box {\nprivate:\n    double width;\npublic:\n    void setWidth(double w) { width = w; }\n    friend void printWidth(const Box& b);\n};\nvoid printWidth(const Box& b) {\n    std::cout << \"Width: \" << b.width << std::endl;\n}"
        ),
        PdfSection(
            heading = "Unit 3: Constructors, Destructors & Object Lifecycle",
            bodyParagraphs = listOf(
                "Constructors initialize object state upon instantiation. If no constructor is defined, the compiler supplies a default constructor.",
                "Types of constructors: Default constructor, Parameterized constructor, Copy constructor (ClassName(const ClassName &obj)), and Dynamic constructors utilizing 'new' operator to allocate memory dynamically.",
                "Destructors (~ClassName) execute automatically when an object leaves scope or is explicitly deleted, deallocating resources."
            ),
            bulletPoints = listOf(
                "Copy Constructor: Invoked during object copy initialization or passing objects by value.",
                "Deep copy vs Shallow copy: Deep copy duplicates heap-allocated buffers to prevent double-free crashes.",
                "Destructors take no arguments, cannot be overloaded, and are invoked in reverse order of construction."
            ),
            codeSnippet = "class Buffer {\n    int *data;\npublic:\n    Buffer(int val) { data = new int(val); }\n    Buffer(const Buffer &b) { data = new int(*b.data); } // Deep Copy\n    ~Buffer() { delete data; } // Destructor cleans up heap\n};"
        ),
        PdfSection(
            heading = "Unit 4: Inheritance, Virtual Base Classes & Polymorphism",
            bodyParagraphs = listOf(
                "Inheritance enables derived classes to acquire characteristics of base classes: Single, Multilevel, Multiple, Hierarchical, and Hybrid inheritance.",
                "Diamond Problem: In multipath inheritance, a derived class inherits multiple copies of a base class. Solved by declaring base inheritance as 'virtual' (Virtual Base Class).",
                "Polymorphism: Compile-time (Function Overloading, Operator Overloading) vs Run-time (Virtual Functions, dynamic binding via vtable)."
            ),
            bulletPoints = listOf(
                "Pure Virtual Function: virtual void draw() = 0 defines an Abstract Base Class.",
                "Virtual Destructor: Ensures derived class destructors are called when deleting derived objects via base pointers.",
                "The 'this' pointer: Implicit pointer passed to non-static member functions referencing the invoking object."
            ),
            codeSnippet = "class Base {\npublic:\n    virtual void show() { std::cout << \"Base\" << std::endl; }\n    virtual ~Base() {}\n};\nclass Derived : public Base {\npublic:\n    void show() override { std::cout << \"Derived\" << std::endl; }\n};"
        ),
        PdfSection(
            heading = "Unit 5: C++ Streams, File I/O & Manipulators",
            bodyParagraphs = listOf(
                "C++ I/O stream hierarchy: ios_base -> ios -> istream / ostream -> iostream. Standard streams cin, cout, cerr, clog are pre-instantiated buffers.",
                "Unformatted I/O uses cin.get(), cout.put(), cin.getline(), and cout.write(). Formatted I/O uses width(), precision(), fill(), and manipulators <iomanip>."
            ),
            bulletPoints = listOf(
                "File streams: ifstream (input), ofstream (output), fstream (bidirectional).",
                "Stream status flags: goodbit, eofbit, failbit, badbit checked via in.good(), in.fail().",
                "Binary file I/O: file.write((char*)&obj, sizeof(obj)) and file.read()."
            ),
            codeSnippet = "#include <fstream>\n#include <iostream>\n\nvoid saveRecord() {\n    std::ofstream out(\"data.txt\");\n    out << \"DevLearn C++ Certification\" << std::endl;\n    out.close();\n}"
        )
    )

    private fun getJavaSections(): List<PdfSection> = listOf(
        PdfSection(
            heading = "Unit 1: Java Virtual Machine (JVM) Architecture & Runtime Memory",
            bodyParagraphs = listOf(
                "Java achieves 'Write Once, Run Anywhere' (WORA) by compiling source code (.java) into platform-independent bytecode (.class) executed by the Java Virtual Machine.",
                "The JVM runtime memory divides into: Method Area (class structures, constant pool), Heap (all instantiated objects, managed by Garbage Collector), Java Thread Stacks (frame allocations, local variables, method invocations), Program Counter (PC) Registers, and Native Method Stacks."
            ),
            bulletPoints = listOf(
                "Classloader Subsystem: Loading (Bootstrap, Extension, Application), Linking (Verification, Preparation, Resolution), and Initialization.",
                "Garbage Collection: Generational hypothesis (Young Generation: Eden/Survivor, Old/Tenured Generation).",
                "Just-In-Time (JIT) Compiler: Identifies hot execution paths and compiles bytecode to native CPU instructions."
            ),
            codeSnippet = "public class JvmArchitectureDemo {\n    public static void main(String[] args) {\n        Runtime rt = Runtime.getRuntime();\n        System.out.println(\"Max Memory: \" + (rt.maxMemory() / (1024 * 1024)) + \" MB\");\n    }\n}"
        ),
        PdfSection(
            heading = "Unit 2: OOP, Interfaces, Generics & Collections Framework",
            bodyParagraphs = listOf(
                "Java enforces strict object orientation: Single class inheritance with multi-interface implementation. Abstract classes define partial implementations, whereas interfaces specify contractual behavior.",
                "The Java Collections Framework (<java.util>) organizes data structures: List (ArrayList, LinkedList), Set (HashSet, TreeSet), Queue (ArrayDeque, PriorityQueue), and Map (HashMap, ConcurrentHashMap, TreeMap)."
            ),
            bulletPoints = listOf(
                "Generics (<T>): Ensures compile-time type safety and eliminates manual casting.",
                "Autoboxing & Unboxing: Automatic conversion between primitives (int, double) and wrapper classes (Integer, Double).",
                "Comparable vs Comparator: Natural ordering vs custom dynamic sorting strategies."
            ),
            codeSnippet = "import java.util.*;\n\nList<String> list = new ArrayList<>();\nlist.add(\"Java\");\nlist.add(\"Kotlin\");\nlist.forEach(System.out::println);"
        ),
        PdfSection(
            heading = "Unit 3: Java Concurrency & Multithreading Systems",
            bodyParagraphs = listOf(
                "Concurrency in Java allows multi-core CPU execution. Threads can be created by extending Thread or implementing Runnable / Callable<V>.",
                "The java.util.concurrent package introduces thread pools (ExecutorService), synchronization locks (ReentrantLock), and atomic variables (AtomicInteger)."
            ),
            bulletPoints = listOf(
                "Thread States: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED.",
                "Volatile keyword: Guarantees visibility across CPU caches without full synchronization locks.",
                "CompletableFuture: Asynchronous, non-blocking pipeline computation."
            ),
            codeSnippet = "import java.util.concurrent.*;\n\nExecutorService executor = Executors.newFixedThreadPool(2);\nexecutor.submit(() -> System.out.println(\"Thread Task Executed\"));\nexecutor.shutdown();"
        )
    )

    private fun getAndroidSections(): List<PdfSection> = listOf(
        PdfSection(
            heading = "Unit 1: Modern Android Architecture & Jetpack Compose Fundamentals",
            bodyParagraphs = listOf(
                "Modern Android application development utilizes declarative UI built on Jetpack Compose, replacing legacy XML layouts with pure Kotlin composable functions.",
                "Architecture follows MVVM (Model-View-ViewModel) and Clean Architecture: Separation of presentation (UI Composables), domain logic (UseCases/ViewModels), and data persistence (Room Database, Repositories, Ktor/Retrofit)."
            ),
            bulletPoints = listOf(
                "Recomposition: Compose intelligently re-executes only functions whose observed state has mutated.",
                "remember & derivedStateOf: Preserving state across recompositions.",
                "Material 3 (M3): Android's modern design language featuring adaptive color and typography."
            ),
            codeSnippet = "@Composable\nfun Greeting(name: String) {\n    var counter by remember { mutableIntStateOf(0) }\n    Button(onClick = { counter++ }) {\n        Text(\"Clicked: \$counter times\")\n    }\n}"
        ),
        PdfSection(
            heading = "Unit 2: Asynchronous State with Kotlin Coroutines, Flow & Room",
            bodyParagraphs = listOf(
                "Kotlin Coroutines provide light-weight, non-blocking threading. Structured concurrency guarantees jobs are canceled appropriately when their lifecycle scope ends.",
                "StateFlow and SharedFlow act as reactive streams. Room provides an abstraction layer over SQLite, emitting Flow<List<Entity>> for real-time observable UI updates."
            ),
            bulletPoints = listOf(
                "Dispatchers: Dispatchers.Main (UI), Dispatchers.IO (disk/network), Dispatchers.Default (CPU-heavy).",
                "viewModelScope: Coroutine scope bounded directly to ViewModel lifecycle.",
                "Room Database: Entities, DAOs (Data Access Objects), and TypeConverters."
            ),
            codeSnippet = "@Dao\ninterface TaskDao {\n    @Query(\"SELECT * FROM tasks\")\n    fun observeTasks(): Flow<List<TaskEntity>>\n    @Insert(onConflict = OnConflictStrategy.REPLACE)\n    suspend fun insertTask(task: TaskEntity)\n}"
        )
    )
}
