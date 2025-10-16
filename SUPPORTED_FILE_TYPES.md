# Supported File Types for Sources

The Sources system now supports **40+ file types** for ingestion and semantic search!

## 📄 PDF Documents

- **Extension:** `.pdf`
- **MIME Type:** `application/pdf`
- **Max Size:** 30 MB (configurable)
- **Max Pages:** 2000 (configurable)
- **Extraction:** PDFBox
- **Features:**
  - Text extraction using PDFBox
  - Thumbnail generation from first page
  - Page-by-page processing
  - Encrypted PDFs not supported

## 📝 Microsoft Office Documents

Max size: 50 MB | Extraction: Apache POI

### Word Documents

| Extension | MIME Type | Description |
|-----------|-----------|-------------|
| `.docx` | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | Word 2007+ documents |
| `.doc` | `application/msword` | Legacy Word documents |

### Excel Spreadsheets

| Extension | MIME Type | Description |
|-----------|-----------|-------------|
| `.xlsx` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | Excel 2007+ spreadsheets |
| `.xls` | `application/vnd.ms-excel` | Legacy Excel spreadsheets |

### PowerPoint Presentations

| Extension | MIME Type | Description |
|-----------|-----------|-------------|
| `.pptx` | `application/vnd.openxmlformats-officedocument.presentationml.presentation` | PowerPoint 2007+ presentations |
| `.ppt` | `application/vnd.ms-powerpoint` | Legacy PowerPoint presentations |

## 📚 OpenDocument Formats

Max size: 50 MB | Extraction: ZIP-based

| Extension | MIME Type | Description |
|-----------|-----------|-------------|
| `.odt` | `application/vnd.oasis.opendocument.text` | OpenDocument Text (LibreOffice Writer) |
| `.ods` | `application/vnd.oasis.opendocument.spreadsheet` | OpenDocument Spreadsheet (LibreOffice Calc) |
| `.odp` | `application/vnd.oasis.opendocument.presentation` | OpenDocument Presentation (LibreOffice Impress) |

## 📖 E-Books & Rich Text

Max size: 50 MB

| Extension | MIME Type | Description |
|-----------|-----------|-------------|
| `.epub` | `application/epub+zip` | EPUB e-books |
| `.mobi` | `application/x-mobipocket-ebook` | Kindle e-books |
| `.rtf` | `application/rtf` | Rich Text Format |

## 🎓 Academic & Publishing

Max size: 50 MB

| Extension | MIME Type | Description |
|-----------|-----------|-------------|
| `.tex` | `application/x-tex` | LaTeX documents |
| `.latex` | `application/x-latex` | LaTeX documents |

## 💻 Programming Languages

Max size: 10 MB | Direct text extraction

| Extension | MIME Type | Description |
|-----------|-----------|-------------|
| `.kt` | `text/x-kotlin` | Kotlin source files |
| `.kts` | `text/x-kotlin-script` | Kotlin script files |
| `.java` | `text/x-java` | Java source files |
| `.py` | `text/x-python` | Python source files |
| `.js` | `text/javascript` | JavaScript files |
| `.ts` | `text/typescript` | TypeScript files |
| `.sh` | `text/x-shellscript` | Shell scripts |
| `.sql` | `text/x-sql` | SQL scripts |

## 🔧 Markup & Configuration

Max size: 10 MB | Direct text extraction

| Extension | MIME Type | Description |
|-----------|-----------|-------------|
| `.md` | `text/markdown` | Markdown documentation |
| `.json` | `application/json` | JSON data files |
| `.xml` | `text/xml` | XML files |
| `.html` | `text/html` | HTML files |
| `.css` | `text/css` | CSS stylesheets |
| `.yaml` | `text/yaml` | YAML configuration |
| `.yml` | `text/yaml` | YAML configuration (alt) |

## 🏗️ Build & Project Files

Max size: 10 MB | Direct text extraction

| Extension | MIME Type | Description |
|-----------|-----------|-------------|
| `.gradle` | `text/x-gradle` | Gradle build scripts |
| `.properties` | `text/x-java-properties` | Java properties files |

## 📊 Data & Logs

Max size: 10 MB | Direct text extraction

| Extension | MIME Type | Description |
|-----------|-----------|-------------|
| `.txt` | `text/plain` | Plain text files |
| `.csv` | `text/csv` | CSV data files |
| `.log` | `text/plain` | Log files |

---

## Summary

| Category | File Types | Count | Max Size |
|----------|-----------|-------|----------|
| **PDF** | `.pdf` | 1 | 30 MB |
| **MS Office** | `.docx`, `.doc`, `.xlsx`, `.xls`, `.pptx`, `.ppt` | 6 | 50 MB |
| **OpenDocument** | `.odt`, `.ods`, `.odp` | 3 | 50 MB |
| **E-Books** | `.epub`, `.mobi`, `.rtf` | 3 | 50 MB |
| **Academic** | `.tex`, `.latex` | 2 | 50 MB |
| **Programming** | `.kt`, `.kts`, `.java`, `.py`, `.js`, `.ts`, `.sh`, `.sql` | 8 | 10 MB |
| **Markup/Config** | `.md`, `.json`, `.xml`, `.html`, `.css`, `.yaml`, `.yml` | 7 | 10 MB |
| **Build Files** | `.gradle`, `.properties` | 2 | 10 MB |
| **Data/Logs** | `.txt`, `.csv`, `.log` | 3 | 10 MB |
| **TOTAL** | - | **40+** | - |

---

## Usage

### Using the API

```kotlin
// Add any supported file
val sourcesEngine = MindModule.provideSourcesEngine(context)
val result = sourcesEngine.addFileFromUri(personaId, fileUri)

// Auto-detects: PDF, DOCX, XLSX, PPTX, Kotlin, Markdown, JSON, and 30+ more!
```

### How It Works

1. **Ingestion:** File is copied to internal storage
2. **Validation:**
   - File size checked against type-specific limits
   - Binary content detection for text files
   - Extension and MIME type validation
3. **Processing:**
   - **PDFs:** PDFBox extraction with page-by-page processing
   - **Office Docs:** Apache POI extraction (DOCX, XLSX, PPTX)
   - **Legacy Office:** ExtractorFactory for .doc, .xls, .ppt
   - **OpenDocument:** ZIP-based XML extraction
   - **E-Books:** HTML/content extraction from archives
   - **Text Files:** Direct UTF-8 reading
4. **Chunking:** Text split into overlapping chunks (1200 chars, 150 overlap)
5. **Embedding:** Gemini API generates 768-dim vectors
6. **Storage:** Vectors quantized to int8 and stored in Room database
7. **Retrieval:** Cosine similarity search for relevant chunks
8. **Context Injection:** Top chunks automatically added to Gemini chat prompts

---

## Adding More File Types

### For Text-Based Files

Edit `TextIngest.SUPPORTED_TYPES`:

```kotlin
val SUPPORTED_TYPES = mapOf(
    // ... existing types ...
    "rs" to "text/x-rust",  // Add Rust
    "go" to "text/x-go",    // Add Go
    "cpp" to "text/x-c++",  // Add C++
)
```

### For Document Files

Edit `DocumentIngest.SUPPORTED_TYPES` and implement extraction in `DocumentExtractor`:

```kotlin
val SUPPORTED_TYPES = mapOf(
    // ... existing types ...
    "pages" to "application/x-iwork-pages-sffpages",  // Apple Pages
)
```

---

## Limitations

- **Binary files:** Images, audio, video not supported (no text content)
- **Encrypted files:** Password-protected PDFs/Office docs not supported
- **Very large files:** May take time to process (background WorkManager)
- **Non-UTF8 text:** May have encoding issues
- **Scanned PDFs:** OCR not implemented (text-based PDFs only)

---

## Features

- ✅ **40+ file types** supported
- ✅ Automatic file type detection
- ✅ Semantic search across all file types
- ✅ Context injection into Gemini chats
- ✅ Real-time embedding with Gemini API
- ✅ Efficient int8 vector storage
- ✅ Background processing with progress tracking
- ✅ Error handling and validation
- ✅ Apache POI for Office documents
- ✅ PDFBox for PDF extraction
- ✅ ZIP-based extraction for OpenDocument/EPUB
- ✅ Fallback extraction methods

---

## Dependencies

- **PDFBox Android** (2.0.27.0) - PDF processing
- **Apache POI** (5.2.5) - Office document processing
- **Gemini API** - Text embeddings
- **Room** - Local vector database
- **WorkManager** - Background indexing
