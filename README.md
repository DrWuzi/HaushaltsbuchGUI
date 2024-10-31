# Haushaltsbuch GUI

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)

## Overview

This project is a Java-based GUI application for managing household expenses. It allows users to add, view, and delete categories and bookings. The application uses a SQLite database to store data and provides various functionalities such as filtering, searching, and custom SQL queries.

## Features

- **Add, View, and Delete Categories**: Manage different categories for transactions.
- **Add, View, and Delete Bookings**: Record and manage household expenses.
- **Search and Filter**: Search bookings by info and filter by date range.
- **Custom SQL Queries**: Execute custom SQL queries to fetch data.
- **Export to Markdown**: Copy table data to clipboard in Markdown format.
- **Set Custom Date**: Manually set a custom date for transactions.

## Project Structure

| File/Directory                | Description                                                                 |
|-------------------------------|-----------------------------------------------------------------------------|
| `src/HaushaltsbuchGUI.java`   | Main GUI class for the application. Handles the main window and its components. |
| `src/UIMenuHelper.java`       | Helper class for UI-related functionalities such as opening dialogs.        |
| `src/DatabaseConnection.java` | Manages the database connection.                                            |
| `src/UIHelper.java`           | Utility class for showing dialogs and other UI-related tasks.               |

## Screenshots

| Feature           | Screenshot                                     |
|-------------------|------------------------------------------------|
| Main Window       | ![Main Window](images/main_window.png)         |
| Add Category      | ![Add Category](images/add_category.png)       |
| Add Booking       | ![Add Booking](images/add_booking.png)         |
| Search and Filter | ![Search and Filter](images/search_filter.png) |
| Custom SQL Query  | ![Custom SQL Query](images/custom_sql.png)     |

## Prerequisites

- **Java Development Kit (JDK)**: Ensure you have JDK 8 or higher installed.
- **SQLite JDBC Driver**: The application uses SQLite for the database.

## Libraries Used

- **Swing**: For building the GUI.
- **JXDatePicker**: For date selection components.
- **SQLite JDBC**: For database connectivity.

## How to Run

1. **Clone the Repository**:
    ```sh
    git clone https://github.com/DrWuzi/HaushaltsbuchGUI.git
    cd HaushaltsbuchGUI
    ```

2. **Open in IntelliJ IDEA**:
    - Open IntelliJ IDEA.
    - Select `Open` and navigate to the cloned repository.

3. **Build the Project**:
    - Ensure all dependencies are resolved.
    - Build the project using `Build > Build Project`.

4. **Run the Application**:
    - Run the `HaushaltsbuchGUI` class.
    - The main window of the application should appear.

## Usage

- **Adding a Category**:
    - Navigate to `Settings > Kategorien`.
    - Fill in the details and click `Speichern`.

- **Adding a Booking**:
    - Fill in the `Info`, `Betrag`, and `Kategorie` fields.
    - Click `Hinzufügen`.

- **Searching and Filtering**:
    - Use the search field and date pickers to filter bookings.

- **Executing Custom SQL**:
    - Click `Custom SQL` and enter your query.
    - Click `Execute` to run the query.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
