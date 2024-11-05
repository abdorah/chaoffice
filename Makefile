# Makefile for Flutter project

# Flutter command
FLUTTER := flutter

# Dart command
DART := dart

# Project name
PROJECT_NAME := chaoffice

# Default target
.PHONY: all
all: clean build

# Clean the project
.PHONY: clean
clean:
	@echo "Cleaning project..."
	@$(FLUTTER) clean
	@rm -rf android
	@rm -rf ios
	@rm -rf linux
	@rm -rf macos
	@rm -rf web
	@rm -rf windows

# Get dependencies
.PHONY: get
get:
	@echo "Getting dependencies..."
	@$(FLUTTER) pub get

# Analyze the project
.PHONY: analyze
analyze:
	@echo "Analyzing project..."
	@$(FLUTTER) analyze

# Run tests
.PHONY: test
test:
	@echo "Running tests..."
	@$(FLUTTER) test

# Build the project
.PHONY: build
build:
	@echo "Building project..."
	@$(FLUTTER) create --platforms windows .
	@$(FLUTTER) build windows

# Run the project
.PHONY: run
run:
	@echo "Running project..."
	@$(FLUTTER) run

# Generate route
.PHONY: route
route:
	@echo "Generating route..."
	@$(DART) pub run build_runner build --delete-conflicting-outputs

# Format code
.PHONY: format
format:
	@echo "Formatting code..."
	@$(DART) format lib

# Update dependencies
.PHONY: update
update:
	@echo "Updating dependencies..."
	@$(FLUTTER) pub upgrade

# Help
.PHONY: help
help:
	@echo "Available targets:"
	@echo "  clean   - Clean the project"
	@echo "  get     - Get dependencies"
	@echo "  analyze - Analyze the project"
	@echo "  test    - Run tests"
	@echo "  build   - Build the project"
	@echo "  run     - Run the project"
	@echo "  route   - Generate route"
	@echo "  format  - Format code"
	@echo "  update  - Update dependencies"
	@echo "  all     - Clean and build the project"