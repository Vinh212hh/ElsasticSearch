✅ 1. Introduction – Why This System Is Needed

Modern systems generate extremely large volumes of log data every day.
Logs contain important information such as:

login attempts

user activity

server errors

system warnings

debugging traces

When thousands of log files each contain tens of thousands of lines, manually searching becomes impossible.

This system was created to automatically generate logs, search through massive datasets, and help developers or system administrators detect important events quickly.

The system provides:

High-volume log generation (for testing)

High-performance parallel searching across thousands of files

Interactive GUI that:

filters files containing matches

displays matching lines

allows users to open a file

highlights keywords inside the file

This results in a complete end-to-end solution for handling large log datasets.

✅ 2. System Workflow Summary
1. Log Generation (SinhLog.java)

The system automatically creates:

3,000 log files

each containing about 20,000 lines

with random events such as login, error, server start, etc.

This simulates a real-world server environment with heavy log activity.

2. Parallel Search Engine (TimKiemSongSong.java)

This program:

scans the log directory

assigns each log file to a worker thread

searches for a keyword (e.g., "login by 99")

writes each result into an output file

ensures high performance by using CPU cores efficiently

3. Search GUI (SearchGui.java)

A desktop application that provides:

user input for keyword and directory

options for case matching or whole-word search

a list of files containing matches

automatic highlight of matching text inside files

a split-pane UI showing:

left → files with hits

right → matching lines

This allows the user to navigate huge log sets easily.

✅ 3. Use Case Diagram

<img width="539" height="560" alt="image" src="https://github.com/user-attachments/assets/48e3d8b7-d387-438d-9bf7-0b5fc1feba12" />

✅ 4. Activity Diagram (Overall System)

<img width="489" height="599" alt="image" src="https://github.com/user-attachments/assets/f1349409-a262-49d5-b519-7bf9978c11a0" />

✅ 5. System Explanation (English)

Here is a clear explanation of how each component works.

5.1. Log Generator (SinhLog.java)

This module simulates a real production environment by generating thousands of log files.

Features:

Automatically creates 3,000 log files

Each file contains 20,000 randomly selected log events

Uses timestamp formatting to mimic realistic server logs

Stores all output in a large directory:
D:\LogData\logs

Purpose:

Used to test the search system with massive datasets.

5.2. Parallel Log Search (TimKiemSongSong.java)

This component efficiently searches for a keyword inside massive numbers of log files.

How it works:

Detects all .txt log files in the target folder

Creates a thread pool based on available CPU cores

Each thread processes one file:

reads it

checks each line for the keyword

accumulates results

Saves output into:

individual result files

summary overview file (tonghop.txt)

Optimizations:

Avoids writing to one file from multiple threads

Stores matches in memory first

Writes at the end to avoid file I/O bottlenecks

This allows scanning 3,000 × 20,000 = 60,000,000 lines quickly.

5.3. Search GUI (SearchGui.java)

A full desktop application built using Swing.

Core features:

Users enter:

keyword

directory to search

options (case sensitive, whole word)

Displays list of files containing the keyword

Displays matching lines

Opens file in a separate window

Highlights all occurrences of the search keyword

Automatically scrolls to the first match

Technical details:

Uses regex patterns for search

Uses background threads to avoid freezing UI

Uses Highlighter to mark matches inside files

Uses efficient block appending to avoid UI lag

This module gives the user a professional search experience similar to Notepad++ "Find in Files".

✅ 6. Why This System Is Useful

This system solves several real-world problems:

1. Log files are huge

In many applications, logs reach gigabytes per day.
Typical tools struggle or freeze when loading large files.

2. Manual searching is impossible

Searching tens of millions of lines by hand is unrealistic.

3. Need for quick troubleshooting

Developers often need to:

detect suspicious login attempts

find failed operations

analyze error patterns

4. Need for test data

SinhLog.java generates massive datasets for performance testing.

5. GUI improves usability

Provides a human-friendly interface to browse and filter results quickly.

My DEMO Video: https://www.youtube.com/watch?v=360aozVUONo



