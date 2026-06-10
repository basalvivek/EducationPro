# Schedule Management Module - Classes Tab Specification

## Purpose

The Classes tab is used to schedule academic sessions between Teachers and their assigned Groups.

The system must only allow scheduling against valid Teacher ? Group assignments created in the Teachers & Assignments module.

This prevents accidental scheduling of unassigned teachers.

---

# User Flow

## Create New Schedule

Admin clicks:

```text
+ New Schedule
```

Selects:

```text
Classes
```

tab.

System opens Class Schedule Form.

---

# Form Layout

Use a modern 2-column layout.

```text
???????????????????????????????????????????????
? Teacher              ? Group                ?
???????????????????????????????????????????????
? Subject              ? Schedule Type        ?
???????????????????????????????????????????????
? Start Date           ? End Date             ?
???????????????????????????????????????????????
? Start Time           ? End Time             ?
???????????????????????????????????????????????
```

---

# Section 1 - Teacher Information

## Teacher

Required

Dropdown

Searchable

```text
Teacher *
```

Example:

```text
John Smith
David Wilson
Sarah Parker
```

---

## Teacher Card

After selection display:

```text
???????????????????????????
? John Smith              ?
? Mathematics Teacher     ?
? Assigned Groups: 4      ?
? Weekly Load: 18 Hours   ?
???????????????????????????
```

---

# Section 2 - Group Information

## Group

Required

Auto-populated.

Only groups assigned to selected teacher should appear.

Example:

```text
Year 7 - Group A
Year 7 - Group B
Year 8 - Group C
```

If no groups assigned:

```text
No Groups Assigned
```

Disable save button.

---

## Group Summary Card

Display:

```text
Group A

Students: 28

Academic Year: 2025

Subjects: 6
```

---

# Section 3 - Subject

Required

Dropdown.

Can be loaded:

### Option A

From teacher assignment.

### Option B

From group curriculum.

Example:

```text
Mathematics
Science
Physics
Chemistry
```

---

# Section 4 - Schedule Type

Dropdown.

```text
Regular Class

Revision Session

Extra Class

Practical Session

Exam Preparation

Parent Session

Workshop
```

---

# Section 5 - Scheduling

## Date Mode

Radio buttons.

```text
Single Day

Multiple Days

Recurring
```

---

# Single Day

Display:

```text
Date

Start Time

End Time
```

Example:

```text
15-Aug-2025

09:00 AM

10:30 AM
```

---

# Multiple Days

Display:

```text
Start Date

End Date
```

Example:

```text
15-Aug

20-Aug
```

System automatically creates entries.

---

# Recurring Schedule

Display recurrence section.

---

## Frequency

Dropdown.

```text
Daily

Weekly

Monthly

Custom
```

---

## Weekly Recurrence

Checkboxes.

```text
Mon
Tue
Wed
Thu
Fri
Sat
Sun
```

Example:

```text
Mon ?

Wed ?

Fri ?
```

---

## End Condition

Radio options.

```text
Never Ends

Until Date

Number of Occurrences
```

---

# Section 6 - Time Slot

## Start Time

Required.

Time Picker.

---

## End Time

Required.

Time Picker.

---

## Duration

Auto-calculated.

Example:

```text
1 Hour 30 Minutes
```

---

# Section 7 - Classroom / Resource

Optional.

---

## Classroom

Dropdown.

Example:

```text
Room A1

Room A2

Science Lab

Computer Lab
```

---

## Equipment

Multi-select.

Example:

```text
Projector

Laptop

Smart Board
```

---

# Section 8 - Lesson Details

## Topic

Required.

Example:

```text
Algebra Basics
```

---

## Description

Rich Text Editor.

Supports:

* Bullet Lists
* Links
* Attachments

---

## Learning Objectives

Chip input.

Example:

```text
Understand Variables

Solve Equations

Apply Algebra Rules
```

---

# Section 9 - Attendance Settings

Checkboxes.

```text
Enable Attendance

Notify Students

Notify Parents

Send Reminder
```

---

# Section 10 - Color Coding

Choose event color.

Examples:

```text
Blue - Mathematics

Green - Science

Purple - English

Orange - Workshops
```

Displayed on calendar.

---

# Conflict Detection

Must validate before save.

---

## Teacher Conflict

Teacher already scheduled.

Show:

```text
Teacher Conflict

John Smith already has a class
09:00 - 10:30
```

---

## Group Conflict

Show:

```text
Group Conflict

Group A already booked.
```

---

## Classroom Conflict

Show:

```text
Room Conflict

Science Lab already reserved.
```

---

# Quick Actions

Bottom of popup.

```text
Save Draft

Save & New

Save Schedule

Cancel
```

---

# Clone Schedule

Available from calendar.

Options:

```text
Clone This Schedule

Clone Entire Week

Clone Entire Month

Clone To Selected Dates
```

---

# Drag & Drop

Admin drags class.

System updates:

```text
Date

Time

Teacher Availability

Group Availability
```

before saving.

---

# Notifications

On successful creation:

Notify:

* Teacher
* Students
* Parents (optional)

---

# Audit Information

Store:

Created By

Created Date

Updated By

Updated Date

Last Modified

Version Number

---

# Calendar Display

Class schedule card:

```text
Mathematics

John Smith

Group A

09:00 - 10:30

Room A1
```

Color-coded and draggable.

---

# Future Integrations

* Attendance Module
* Assessment Designer
* Resource Booking
* Parent Portal
* Student Portal
* Teacher Dashboard

The Classes tab should act as the central academic scheduling engine for the entire platform.
