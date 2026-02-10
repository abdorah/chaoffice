# ChaOffice - Parts Inventory Management System

## Introduction

ChaOffice is a comprehensive desktop application for managing parts inventory, sales, and reporting. Built with JavaFX, it provides an intuitive interface for tracking inventory, processing sales transactions, generating reports, and managing business operations.

## Features

- **Multi-language Support**: English, Arabic (العربية), and French (Français)
- **Inventory Management**: Track parts, categories, and stock levels
- **Sales & Billing**: Create bills with optional client information, discounts, and multiple payment methods
- **Reports & Analytics**: Generate sales and inventory reports in PDF, CSV, and Excel formats
- **User Management**: Secure authentication and user roles
- **Database**: SQLite-based local storage

## Technology Stack

- **JavaFX 22.0.2**: Modern desktop UI framework
- **Java 17**: Core programming language
- **SQLite 3.44.1**: Embedded database
- **Apache POI 5.2.5**: Excel file handling
- **OpenPDF 2.0.2**: PDF generation
- **SLF4J 2.0.9**: Logging framework
- **JUnit 5**: Testing framework

---

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Run the Application (Development)
```bash
mvn javafx:run
```

---

## 📦 Building for Distribution

### Option 1: Fat JAR (Recommended - Easiest)

**Best for**: Quick distribution, technical users

```bash
mvn clean package
```

**Output**: `target/chaoffice-1.0.0.jar` (~50-80 MB)

**Run**:
```bash
java -jar target/chaoffice-1.0.0.jar
```

**Requirements**: Java 17+ on target machine

---

### Option 2: Standalone Application with jpackage (Best for End Users)

**Best for**: Professional distribution, no Java required

**Step 1: Build the JAR**
```bash
mvn clean package
```

**Step 2: Create Standalone Package**

**Windows:**
```bash
jpackage --input target --name "ChaOffice" --main-jar chaoffice-1.0.0.jar --main-class org.chaos.office.ChaOfficeApplication --type app-image --app-version 1.0.0 --vendor "ChaOffice" --description "Parts Inventory Management System"
```

**Linux:**
```bash
jpackage --input target --name "ChaOffice" --main-jar chaoffice-1.0.0.jar --main-class org.chaos.office.ChaOfficeApplication --type app-image --app-version 1.0.0 --vendor "ChaOffice" --description "Parts Inventory Management System"
```

**Mac:**
```bash
jpackage --input target --name "ChaOffice" --main-jar chaoffice-1.0.0.jar --main-class org.chaos.office.ChaOfficeApplication --type app-image --app-version 1.0.0 --vendor "ChaOffice" --description "Parts Inventory Management System"
```

**Output**: `ChaOffice/` folder with standalone application

**Run**:
- Windows: `ChaOffice\ChaOffice.exe`
- Linux/Mac: `./ChaOffice/bin/ChaOffice`

**Requirements**: None! Everything included

---

### Option 3: Native Installers

**Windows Installer (.msi):**
```bash
mvn clean package
jpackage --input target --name "ChaOffice" --main-jar chaoffice-1.0.0.jar --main-class org.chaos.office.ChaOfficeApplication --type msi --win-menu --win-shortcut --app-version 1.0.0
```

**Linux Package (.deb):**
```bash
mvn clean package
jpackage --input target --name "ChaOffice" --main-jar chaoffice-1.0.0.jar --main-class org.chaos.office.ChaOfficeApplication --type deb --app-version 1.0.0
```

**Mac Installer (.dmg):**
```bash
mvn clean package
jpackage --input target --name "ChaOffice" --main-jar chaoffice-1.0.0.jar --main-class org.chaos.office.ChaOfficeApplication --type dmg --app-version 1.0.0
```

---

### Option 4: Automated Build Scripts

**Windows:**
```bash
build-release.bat
```

**Linux/Mac:**
```bash
chmod +x build-release.sh
./build-release.sh
```

These scripts automatically:
1. Clean previous builds
2. Run all tests
3. Create fat JAR
4. Create standalone application with jpackage
5. Package everything as ZIP for distribution

---

## ⚠️ Known Issue: jlink Hash Mismatch

If you encounter this error when trying `mvn javafx:jlink`:
```
Error: Hash of javafx.base differs to expected hash recorded in java.base
```

**This is a known JavaFX module compatibility issue.** Use one of these alternatives instead:

### Solution 1: Use Fat JAR (Easiest)
```bash
mvn clean package
```

### Solution 2: Use jpackage (Recommended)
```bash
mvn clean package
jpackage --input target --name "ChaOffice" --main-jar chaoffice-1.0.0.jar --main-class org.chaos.office.ChaOfficeApplication --type app-image
```

### Solution 3: Match JavaFX Version to JDK

Check your JDK version:
```bash
java -version
```

Update `pom.xml` to match:
- JDK 17 → `<javafx.version>17.0.2</javafx.version>`
- JDK 21 → `<javafx.version>21.0.1</javafx.version>`

**See `JLINK_ISSUE_SOLUTIONS.md` for detailed information.**

---

## 📁 Build Output

After building, you'll find:

```
target/
├── chaoffice-1.0.0.jar              # Fat JAR (ready to run)
└── ChaOffice-1.0.0-windows.zip      # Distribution package

ChaOffice/                            # Standalone application (if jpackage used)
├── bin/
│   ├── ChaOffice.exe                # Windows launcher
│   └── ChaOffice                    # Linux/Mac launcher
└── lib/                             # Application libraries
```

---

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=BillServiceTest
```

### Skip Tests
```bash
mvn clean package -DskipTests
```

---

## 📊 Build Comparison

| Method | Output | Size | Java Required? | Best For |
|--------|--------|------|----------------|----------|
| Fat JAR | Single .jar file | ~50-80 MB | Yes (17+) | Quick distribution |
| jpackage | Standalone app | ~100-150 MB | No | End users |
| Installer | Native installer | ~100-200 MB | No | Enterprise deployment |

---

## 🎯 Recommended Build Strategy

### For Development/Testing:
```bash
mvn javafx:run
```

### For Beta Testing:
```bash
mvn clean package
# Share: target/chaoffice-1.0.0.jar
```

### For Production Release:
```bash
./build-release.sh  # or .bat on Windows
# Share: target/ChaOffice-1.0.0-windows.zip
```

### For Enterprise Deployment:
```bash
mvn clean package
jpackage --input target --name "ChaOffice" --main-jar chaoffice-1.0.0.jar --main-class org.chaos.office.ChaOfficeApplication --type msi --win-menu --win-shortcut
# Share: ChaOffice-1.0.0.msi
```

---

## 📚 Documentation

- **PACKAGING_GUIDE.md** - Comprehensive packaging guide
- **QUICK_START_PACKAGING.md** - Quick reference for building
- **JLINK_ISSUE_SOLUTIONS.md** - Solutions for jlink errors
- **INSTALLATION_GUIDE.md** - End-user installation instructions
- **LOCALIZATION_UPDATES.md** - Multi-language support details
- **CHANGES_OPTIONAL_CLIENT_FIELDS.md** - Optional fields feature

---

## 🔧 System Requirements

### Development:
- Java 17 or higher
- Maven 3.6+
- Windows 10+, Linux, or macOS

### End Users (JAR):
- Java 17 or higher

### End Users (Standalone):
- No requirements! Everything included
- Windows 10+, Linux (Ubuntu 20.04+), or macOS 10.14+

---

## 🌍 Multi-Language Support

The application supports three languages:
- **English** (Default)
- **Arabic** (العربية) - with RTL support
- **French** (Français)

Language can be changed in Settings.

---

## ✨ Recent Updates (v1.0.0)

- ✅ Optional client name and phone fields in billing
- ✅ Complete Arabic and French translations
- ✅ Enhanced reports with multiple export formats (PDF, CSV, Excel)
- ✅ Improved inventory management
- ✅ Sales analytics and reporting
- ✅ Multi-language support

---

## 🆘 Troubleshooting

### Build Fails
```bash
mvn clean
mvn compile
mvn package
```

### Tests Fail
```bash
mvn clean package -DskipTests
```

### "Module not found" Error
Use `mvn javafx:run` instead of `java -jar`

### Can't Run JAR
Ensure Java 17+ is installed:
```bash
java -version
```

### jlink Hash Mismatch
See **JLINK_ISSUE_SOLUTIONS.md** or use fat JAR/jpackage instead

---

## 📞 Support

For issues, questions, or feature requests:
- Check documentation in project root
- Review FAQ and troubleshooting guides
- Submit issues on the project repository

---

## 📝 Version Information

**Current Version**: 1.0.0  
**Release Date**: February 2026  
**Java Version**: 17  
**JavaFX Version**: 22.0.2  

---

## 🏆 Key Features

### Inventory Management
- Track parts with categories and makers
- Real-time stock level monitoring
- Low stock alerts
- Image support for parts

### Sales & Billing
- Create bills with optional client information
- Multiple discount types (percentage, fixed amount)
- Multiple payment methods (cash, card, check)
- Editable prices per transaction
- Stock validation

### Reports & Analytics
- Sales reports with date ranges
- Inventory reports with stock thresholds
- Top selling parts analysis
- Payment method breakdown
- Export to PDF, CSV, and Excel

### User Interface
- Modern, intuitive design
- Dark mode support
- Responsive layouts
- Multi-language interface

---

## 🔐 Security

- Secure user authentication
- Encrypted password storage
- Session management
- Audit logging for transactions

---

## 📄 License

[Your License Here]

---

## 🙏 Credits

Developed by [Your Name/Organization]

---

## 🚀 Getting Started

1. **Clone the repository**
2. **Build the application**: `mvn clean package`
3. **Run it**: `java -jar target/chaoffice-1.0.0.jar`
4. **Default login**: username: `admin`, password: `admin`
5. **Change password** after first login!

---

**Happy Inventory Managing!** 🎉
