# Design Courses Screen - Premium SaaS UI Redesign Prompt

## Context

Redesign the existing **Design Courses** page while preserving its current workflow and information architecture.

DO NOT completely change the layout.

Keep:

* Left Navigation Sidebar
* Course Tree Panel
* Node Details Editor Panel
* Top Header
* Save/Delete Actions

The goal is to transform the page from a traditional admin form into a premium, modern, visually impressive SaaS experience.

Think:

* Linear
* Notion
* Stripe Dashboard
* Framer
* Airtable

---

# Main Problems In Current Design

The current UI suffers from:

* Flat appearance
* Weak visual hierarchy
* Excessive white space
* Tree view looks outdated
* Forms look like standard Bootstrap inputs
* Lack of visual engagement
* No meaningful use of illustrations
* Actions are not visually prominent
* Content editor feels generic

The page should feel premium and presentation-ready.

---

# Overall Design Direction

## Style

Modern Educational SaaS Platform

### Keywords

* Premium
* Clean
* Elegant
* Minimal
* Spacious
* Modern
* Professional
* Enterprise Ready

---

# Color Palette

Primary

```css
#4F46E5
```

Primary Light

```css
#EEF2FF
```

Secondary

```css
#06B6D4
```

Success

```css
#22C55E
```

Danger

```css
#EF4444
```

Background

```css
#F8FAFC
```

Card

```css
#FFFFFF
```

Border

```css
#E2E8F0
```

---

# Layout Structure

Keep the same 3-panel layout.

```text
???????????????????????????????????????????????
? Header                                      ?
???????????????????????????????????????????????
?            ?                 ?              ?
? Navigation ? Course Tree     ? Node Editor  ?
? Sidebar    ?                 ?              ?
?            ?                 ?              ?
???????????????????????????????????????????????
```

---

# Sidebar Redesign

Transform the dark sidebar into a premium navigation experience.

## Improvements

* Soft gradient background
* Modern icons
* Hover glow effect
* Active menu indicator
* Smooth transitions

Example gradient:

```css
background:
linear-gradient(
180deg,
#0F172A,
#1E1B4B
);
```

---

## Active Menu

Instead of a simple blue highlight:

Use:

* Rounded pill
* Soft shadow
* Gradient background

Example:

```text
? Design Courses
```

with glow effect.

---

## Upgrade Card

Replace basic card.

Create:

```text
Upgrade to Pro

Unlock AI Course Builder
Analytics
Question Recommendations

[ Upgrade Now ]
```

Use gradient background.

---

# Course Tree Panel

This is the most important area.

Convert it into a modern explorer.

---

## Header

Current:

```text
Course Tree
```

Replace with:

```text
?? Course Explorer
```

Add:

* Total Nodes Count
* Search
* Quick Add

Example:

```text
Course Explorer

42 Nodes

[ Search ]

[ + Add Course ]
```

---

## Search

Modern search box.

Rounded:

```css
border-radius: 14px;
```

Icon inside input.

---

## Tree Items

Current tree looks old-fashioned.

Replace with rich node rows.

---

### Class Node

```text
?? Class 8
```

Blue badge

---

### Subject Node

```text
?? Mathematics
```

Purple badge

---

### Topic Node

```text
?? Geometry
```

Cyan badge

---

### Question Node

Use colorful icons.

Example:

```text
?? Explain

?? MCQ

?? True / False

?? Image MCQ
```

---

### Selected Node

Current highlight is weak.

Use:

* Purple background
* Left border
* Glow effect
* Slight elevation

---

# Node Details Panel

Make this the hero section.

---

## Hero Banner

Current banner is small.

Create a premium hero section.

Example:

```text
Edit Node Information

Manage course content,
learning outcomes,
resources and assessments.
```

Include:

* Modern illustration
* Gradient background
* Floating decorative shapes

Height:

```css
180px
```

---

## Form Container

Place inside elevated card.

```css
border-radius: 18px;
box-shadow:
0 10px 30px rgba(0,0,0,.06);
```

---

# Inputs

Current inputs look generic.

Convert all inputs into modern fields.

Features:

* Floating labels
* Focus glow
* Soft background

Example:

```css
background: #FAFAFF;
border: 1px solid #E2E8F0;
```

---

# Description Editor

Current editor looks plain.

Transform into premium content editor.

Add:

* Modern toolbar
* Better spacing
* Drag and drop image support
* Character count
* AI assist button

Example:

```text
? Improve Content
```

---

# Tagline Section

Convert into suggestion field.

Example:

```text
Tagline

"Master the fundamentals of triangles and geometry."
```

Add:

```text
Generate Suggestion
```

button.

---

# Statistics Panel

Below form add metrics.

Example:

```text
Questions
24

Resources
12

Students
186

Completion
82%
```

Display as cards.

---

# Action Buttons

Current buttons are basic.

Replace with premium actions.

---

## Save

Gradient button

```css
linear-gradient(
135deg,
#4F46E5,
#7C3AED
);
```

Text:

```text
Save Changes
```

---

## Delete

Outline button

```css
border: 1px solid #EF4444;
```

---

## Add New

Floating action button.

---

# Metadata Card

Current created/updated section looks disconnected.

Replace with modern activity card.

Example:

```text
Activity

Created
12 May 2024

Last Updated
20 May 2024

Published By
Admin
```

Include icons.

---

# Micro Interactions

Add:

* Tree expand animation
* Hover elevation
* Button ripple
* Input glow
* Toast notifications
* Smooth transitions
* Skeleton loaders

Duration:

```css
200ms - 300ms
```

---

# Empty State

When no node selected:

Show illustration.

```text
No Node Selected

Choose a course item from the explorer
or create a new one.
```

Button:

```text
Create New Node
```

---

# Responsive Behavior

Desktop

3 Panels

Tablet

Sidebar collapses

Mobile

Drawer Navigation

Tree panel becomes expandable drawer

Editor becomes full width

---

# Final Goal

The redesigned screen should feel like a premium curriculum management platform used by universities, schools, and enterprise learning providers.

The user should immediately feel:

* Modern
* Professional
* Premium
* Easy to use
* Presentation ready

Avoid anything that looks like Bootstrap, AdminLTE, Metronic, or a traditional CRUD form.
