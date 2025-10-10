# Outdated Documentation Archive

**Archive Date**: 2025-10-10
**Reason**: Replaced by standardized documentation in `docs/claudecode/`

---

## Purpose

This directory contains legacy documentation that has been superseded by the standardized documentation framework based on claude-code-subagents templates.

**Do not reference or update these files.** They are kept for historical reference only.

---

## Current Documentation Location

All active documentation is now located in:
- **Architecture**: `docs/claudecode/arch.md`
- **Development Rules**: `docs/claudecode/develop_rules.md`
- **Solution Guide**: `docs/claudecode/solution.md`
- **Work Breakdown**: `docs/claudecode/WBS.md`
- **Backend Docs**: `docs/claudecode/Backend.md`
- **Frontend Docs**: `docs/claudecode/Frontend.md`

---

## Archived Files

The following files were moved here on 2025-10-10:

### Architecture Documents
- `ARCHITECTURE.md` → Superseded by `docs/claudecode/arch.md`
- `PORT_ALLOCATION_MASTER.md` → Port information now in `docs/claudecode/arch.md`

### Migration & Planning
- `MIGRATION_PLAN.md` → Azure migration info now in `docs/claudecode/arch.md` and `docs/claudecode/solution.md`
- `IMPLEMENTATION.md` → Implementation details now in work breakdown and backend docs
- `PROGRESS_TRACKING.md` → Project tracking now in WBS and status files

### Feature & Data
- `FEATURE_MAPPING.md` → Feature mapping integrated into architecture and solution docs
- `DATA_MIGRATION.md` → Data migration strategy now in solution guide
- `API_EXTRACTION_GUIDE.md` → API documentation now in architecture doc

### Cost Analysis
- `COST_ANALYSIS.md` → Cost analysis preserved in migration plan section
- `POC_COST_ANALYSIS.md` → POC cost info preserved in migration plan

---

## Migration Notes

### Key Improvements in New Documentation

1. **Standardization**: Based on proven templates from claude-code-subagents
2. **Role-Based**: Supports @arch, @back, @front, @pm, @reviewer, @test roles
3. **Consistency**: Follows industry best practices
4. **Maintainability**: Clear ownership and review schedules
5. **Completeness**: Comprehensive coverage of all aspects

### What Was Consolidated

- **Multiple architecture docs** → Single `arch.md`
- **Scattered migration info** → Integrated into `solution.md`
- **Port allocation** → Part of architecture doc
- **Implementation details** → Backend and frontend specific docs

### What Was Added

- **Development standards** → `develop_rules.md`
- **Coding conventions** → Clear style guidelines
- **Testing requirements** → Minimum 70% coverage mandate
- **Security best practices** → Input validation, SQL injection prevention
- **Performance guidelines** → Database optimization, caching strategy

---

## When to Reference These Files

These archived files should only be referenced:
1. For historical context
2. To understand past decisions
3. To migrate remaining information not yet captured

**Always verify information against current documentation in `docs/claudecode/` before use.**

---

## Cleanup Schedule

These files will be permanently deleted after **2025-12-31** (90-day retention period).

If you find information in these files that should be preserved, please:
1. Extract the relevant content
2. Add it to the appropriate current documentation
3. Submit a pull request or notify @arch

---

**Archive Maintained By**: @arch
**Contact**: See project documentation for questions
**Last Updated**: 2025-10-10
