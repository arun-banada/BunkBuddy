# BunkBuddy

BunkBuddy is an AI-powered smart attendance prediction system for college students. It helps track attendance, predict safe bunking opportunities, and extracts subjects from timetables using OCR.

## Features

- **Smart Attendance Dashboard**: View overall attendance, total classes, and risky subjects.
- **AI Recommendations**: Get calculated safe bunks and recommendations to stay above 75%.
- **Subject Management**: Add subjects, track attendance individually with Present/Absent buttons.
- **Timetable OCR**: Upload an image of your timetable to automatically extract subject names.
- **Modern UI**: Dark mode, glassmorphism UI built with React, Tailwind CSS, and Framer Motion.

## Tech Stack

- **Frontend**: React.js, Vite, Tailwind CSS, Recharts, Framer Motion
- **Backend**: Spring Boot 3, Spring Security, JWT, Maven
- **Database**: MySQL
- **OCR Engine**: Tesseract (Tess4J)

## Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- MySQL Server running on port 3306

### Database Setup

1. The backend is configured to create the database automatically (`createDatabaseIfNotExist=true`).
2. Make sure your MySQL root user has the password `root`. You can change this in `bunkbuddy-backend/src/main/resources/application.properties`.

### Running the Backend

1. Navigate to the backend directory: `cd bunkbuddy-backend`
2. If using OCR, place your `tessdata` folder inside `bunkbuddy-backend/tessdata`. (Note: The OCR is mocked to return dummy subjects if it fails so it works out of the box without tessdata for demo purposes).
3. Run the Spring Boot application: `./mvnw spring-boot:run`
4. The server will start on `http://localhost:8080`.

### Running the Frontend

1. Navigate to the frontend directory: `cd bunkbuddy-frontend`
2. Install dependencies: `npm install`
3. Start the dev server: `npm run dev`
4. Open your browser and go to `http://localhost:5173`.

## Testing

You can use the built-in Registration page to create a new dummy student account, and then log in to explore the features!

### API Documentation

Authentication is required for all endpoints except `/api/auth/register` and `/api/auth/login`. Pass the JWT token as `Authorization: Bearer <token>`.

- `POST /api/auth/login`: Login
- `POST /api/auth/register`: Register new user
- `GET /api/dashboard`: Fetch analytics stats
- `GET /api/subjects`: Get all subjects and predictions
- `POST /api/subjects`: Add a new subject
- `POST /api/attendance/mark`: Mark attendance (Present/Absent)
- `POST /api/timetable/upload`: Upload timetable file (multipart/form-data)
