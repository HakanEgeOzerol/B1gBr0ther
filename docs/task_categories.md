# Task Categories Documentation

## Overview

The task categories feature in B1gBr0ther allows you to organize and filter your tasks by different categories such as work, study, personal, etc. This makes it easier to manage your activities and analyze your productivity patterns.

## Available Categories

The following categories are available for tasks:

- **WORK**: Professional or work-related tasks
- **STUDY**: Educational activities or study sessions
- **PERSONAL**: Personal tasks and errands
- **HEALTH**: Health-related activities such as exercise, appointments, etc.
- **FAMILY**: Family-related responsibilities
- **HOBBY**: Leisure or hobby activities
- **OTHER**: Tasks that don't fit into other categories

## Using Categories

### Creating Tasks with Categories

1. When creating a new task, you'll be prompted to select a category from the dropdown menu.
2. If no category is selected, it will default to "OTHER".
3. Categories can't be empty - each task must have a category assigned.

### Filtering Tasks by Category

1. Go to the Export Page where task lists are displayed.
2. Use the Category filter dropdown to view tasks from a specific category.
3. Select "All Categories" to view tasks from all categories.

### Task Category Statistics

1. Navigate to the Statistics screen.
2. View the Task Categories chart to see the distribution of your tasks across different categories.
3. The chart shows both the count and percentage of tasks in each category.

## Color Coding

Task categories are color-coded throughout the app to make them easier to identify:

- WORK: Orange (#FF5722)
- STUDY: Blue (#2196F3)
- PERSONAL: Purple (#9C27B0)
- HEALTH: Green (#4CAF50)
- FAMILY: Pink (#E91E63)
- HOBBY: Yellow (#FFEB3B)
- OTHER: Blue Grey (#607D8B)

## Technical Implementation

Categories are implemented as an enum class `TaskCategory` and are stored in the database as strings. The Room database uses a TypeConverter to convert between the enum values and database strings.

Task filtering by category is implemented in the ExportPage activity and tasks can be queried from the database by their category.
