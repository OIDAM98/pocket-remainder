
- [Pocket Reminder](#pocket-reminder)
  - [Description](#description)
  - [Objective](#objective)
  - [Missing Tasks](#missing-tasks)
  - [Dependencies (*TBA*)](#dependencies-tba)
    - [Core](#core)
    - [CLI](#cli)
    - [Client-Server App](#client-server-app)
    - [Resources Used (*TBA*)](#resources-used-tba)

# Pocket Reminder

A basic application to remind reading through old links saved to [Pocket](https://getpocket.com/) in order to clear the reading list or categorize them as *favorites*.

The application can show the user a designated number or random old links that have yet to be read from the user's list. Also it can send this list through email for the user to in the browser or Pocket's app.

## Description
Pocket Reminder is both a simple library and an application.
The library is used to connect to Pocket's API and manage the user's account through it using pure Scala.
The purpose of the application is to remind the user to read though old links saved to Pocket to clean its reading list or save important links that were not previously marked.

## Objective
The focus of the project is mainly to learn about pure functional programming, using *Cats*, and Hexagonal Architecture principles. The idea for the project was born from a personal need, making it a great fit as a playground for these new ideas.

The main goal of this project is to create a simple library to manage some Pocket API's actions through Scala. This library will have 2 main clients: one Command-Line Interface and a Client-Server application. Each client will extend the core library as needed.

## Missing Tasks

- Core
  - Add *remove from Pocket list* functionality
  - Add *algebra* for recurrent tasks
- CLI
  - Add *interpreter* to set recurrent tasks
  - Change from manual read to use ***Ciris*** library for config read
  - Dockerize CLI
- Create Client-Server app
  - Create FrontEnd
  - Create Backend
    - Implement interpreters
    - Create Server
      - Design API
      - Implement needed routes
  - Create pipeline for server to serve frontend
  - Deploy?

## Dependencies (*TBA*)

### Core

### CLI

### Client-Server App

### Resources Used (*TBA*)

