# Unicode Filename Handling Unit Tests

This document describes the comprehensive unit tests created for the unicode filename fix (commit: 37bd5e53763a698904d2f04f018b7812101257a5).

## Background

The original issue ([#596](https://github.com/adityachandelgit/BookLore/issues/596)) was that BookLore crashed when processing files with unicode characters in their filenames, specifically:

```
Processing file: Integrated Chinese 中文听说读写 Text - Yuehua Liu.pdf
booklore exited with code 0
```

## Test Coverage

### 1. PdfProcessorUnicodeTests
**Location**: `booklore-api/src/test/java/com/adityachandel/booklore/service/fileprocessor/PdfProcessorUnicodeTests.java`

**What it tests**:
- PDF files with unicode characters in filename are processed correctly
- Full path is used instead of just filename (key fix)
- Various unicode character sets (Chinese, Japanese, Russian, Arabic, Hindi, Korean)
- Force processing vs existing file handling
- Error scenarios

**Key test cases**:
- `processFileWithUnicodeFilename_shouldUseFullPath()` - Verifies the core fix
- `processFileWithUnicodeFilename_shouldNotThrowException()` - Tests various unicode charsets
- `processFileWithUnicodeFilename_forceProcess_shouldProcessEvenIfExists()` - Force processing
- `processFileWithUnicodeFilename_existingFile_shouldReturnExisting()` - Existing file handling

### 2. EpubProcessorUnicodeTests
**Location**: `booklore-api/src/test/java/com/adityachandel/booklore/service/fileprocessor/EpubProcessorUnicodeTests.java`

**What it tests**:
- EPUB files with unicode characters in filename are processed correctly
- Full path usage for EPUB processing
- Nested unicode directories with unicode filenames
- Extended unicode character sets including Greek and Hebrew

**Key test cases**:
- `processFileWithUnicodeFilename_shouldUseFullPath()` - Core EPUB unicode handling
- `processFileWithVariousUnicodeFilenames_shouldNotThrowException()` - Extended charset testing
- `processFileWithUnicodeFilename_withNestedDirectories_shouldUseCorrectFullPath()` - Complex path handling

### 3. CbxProcessorUnicodeTests
**Location**: `booklore-api/src/test/java/com/adityachandel/booklore/service/fileprocessor/CbxProcessorUnicodeTests.java`

**What it tests**:
- Comic book archive files (CBZ, CBR, CB7) with unicode filenames
- Archive extraction with unicode entry names
- Different archive formats with unicode handling
- Mock archive creation for testing

**Key test cases**:
- `processFileWithUnicodeFilename_shouldUseFullPath()` - Core CBX unicode handling
- `processFileWithVariousUnicodeArchiveFormats_shouldNotThrowException()` - Multiple archive formats
- `processFileWithUnicodeFilename_withUnicodeArchiveEntries_shouldHandle()` - Unicode archive contents
- `processFileWithUnicodeFilename_withNestedDirectories_shouldUseCorrectFullPath()` - Complex nested paths

### 4. LibraryProcessingServiceUnicodeTests
**Location**: `booklore-api/src/test/java/com/adityachandel/booklore/service/library/LibraryProcessingServiceUnicodeTests.java`

**What it tests**:
- Overall library processing with unicode filenames
- Charset logging functionality
- File type detection with unicode filenames
- Error handling during unicode file processing
- Multiple file processing

**Key test cases**:
- `init_shouldLogCharsetInformation()` - Charset logging verification
- `processLibraryFile_withUnicodePdfFilename_shouldUsePdfProcessor()` - Routing to correct processor
- `processLibraryFiles_withMultipleUnicodeFilenames_shouldProcessAll()` - Batch processing
- `getBookFileType_withUnicodeFilenames_shouldDetectCorrectTypes()` - File type detection

### 5. LibraryFileUnicodeTests
**Location**: `booklore-api/src/test/java/com/adityachandel/booklore/model/dto/settings/LibraryFileUnicodeTests.java`

**What it tests**:
- LibraryFile.getFullPath() method with unicode characters
- Various unicode character sets and combinations
- Mixed ASCII and Unicode in paths
- Special characters in filenames

**Key test cases**:
- `getFullPath_withUnicodeFilename_shouldReturnCorrectPath()` - Basic unicode path construction
- `getFullPath_withUnicodeDirectoryAndFilename_shouldReturnCorrectPath()` - Nested unicode directories
- `getFullPath_withVariousUnicodeCharsets_shouldHandleAllCorrectly()` - Comprehensive charset testing
- `getFullPath_withMixedEncodingInPath_shouldHandleCorrectly()` - Mixed encoding scenarios

## Test Features

### Unicode Character Sets Tested
- **Chinese**: 中文听说读写, 普通话教程, 中文书籍
- **Japanese**: 日本語, Japonés básico 日本語
- **Korean**: 한국어, 만화, 기초
- **Russian**: Русский язык, комикс, учебник
- **Arabic**: العربية للمبتدئين, كوميكس
- **Hindi**: हिंदी भाषा सीखें, कॉमिक
- **Greek**: Ελληνικά για αρχάριους, κόμικς
- **Hebrew**: עברית למתחילים, קומיקס
- **Turkish**: Türkçe öğrenme kitabı
- **Portuguese**: Português básico
- **French**: Français débutant naïve café

### Testing Patterns Used
- **JUnit 5** with `@ExtendWith(MockitoExtension.class)`
- **Mockito** for mocking dependencies
- **AssertJ** for fluent assertions
- **@TempDir** for file system testing
- **@Mock** annotations for dependency injection

### Mock Strategies
- Repository mocking for database interactions
- Service layer mocking for business logic
- File system operations with temporary directories
- Archive file creation for CBX testing

## How to Run Tests

```bash
cd booklore-api
./gradlew test --tests "*Unicode*"
```

Or run individual test classes:
```bash
./gradlew test --tests "PdfProcessorUnicodeTests"
./gradlew test --tests "EpubProcessorUnicodeTests"
./gradlew test --tests "CbxProcessorUnicodeTests"
./gradlew test --tests "LibraryProcessingServiceUnicodeTests"
./gradlew test --tests "LibraryFileUnicodeTests"
```

## Key Assertions Verified

1. **Full Path Usage**: All processors now use `libraryFile.getFullPath()` instead of just filename
2. **No Exceptions**: Unicode filenames don't cause crashes
3. **Correct Processing**: Files are processed and saved correctly
4. **Path Construction**: LibraryFile.getFullPath() correctly handles unicode paths
5. **File Type Detection**: Unicode filenames don't interfere with file type detection
6. **Archive Handling**: CBX processor correctly handles unicode archive names and contents

## Integration with Original Fix

These tests verify the key changes made in commit 37bd5e53763a698904d2f04f018b7812101257a5:

1. **UTF-8 JVM Encoding**: `java -Dfile.encoding=UTF-8` in Dockerfile
2. **Full Path Usage**: Changed from `new File(libraryFile.getFileName())` to `new File(libraryFile.getFullPath().toString())`
3. **Enhanced Logging**: Comprehensive logging for unicode filename processing
4. **Archive Library Improvements**: Better path handling for ZipFile and SevenZFile

## Expected Test Results

All tests should pass, demonstrating that:
- Unicode filenames are handled correctly across all file processors
- No crashes occur during unicode file processing
- Full paths are properly constructed and used
- Archive operations work with unicode filenames
- The fix successfully resolves issue #596 