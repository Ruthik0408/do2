import React from "react";

import { DataComponent, DataTable, ReportSection, RichNarrative, useDataApp } from "../../data-app-public.jsx";

const coverageColumns = [
  { key: "measure", label: "Static evidence" },
  { key: "count", label: "Count" },
  { key: "interpretation", label: "Interpretation" },
];
const statusColumns = [
  { key: "status", label: "Status" },
  { key: "setter", label: "Code setter" },
  { key: "occurrences", label: "Occurrences" },
  { key: "code_can_assign", label: "Code can assign", presentation: "status" },
];
const aadhaarColumns = [
  { key: "processor", label: "Processor" },
  { key: "check", label: "Automatic checks" },
  { key: "automatic_status", label: "Code result" },
  { key: "duplicate_do2_check", label: "Duplicate check" },
];

export function ReportContent() {
  const { snapshot, appTitle, canEdit, mode, setAppTitle } = useDataApp();
  const coverage = snapshot.queries.validation_coverage.rows;
  const statuses = snapshot.queries.status_assignment.rows;
  const aadhaar = snapshot.queries.aadhaar_examples.rows;

  return <article className="report-content" aria-label="DO2 validation and status provenance report">
    <header className="report-hero">
      <h1 data-data-app-title contentEditable={canEdit && mode === "edit"} suppressContentEditableWarning
        onBlur={canEdit && mode === "edit" ? (event) => setAppTitle(event.currentTarget.textContent.trim() || appTitle) : undefined}>
        {appTitle}
      </h1>
      <RichNarrative id="report:introduction" className="report-deck"
        value="A repository-wide static review shows that application code can assign every operational status used for ML labels. Rows left after matching only AU, VL, DU, or a null reason therefore cannot safely be assumed to be human-reviewed." />
    </header>

    <ReportSection id="report-summary" title="Decision" queryId="validation_coverage" sourceRows={coverage} showHeading={false}>
      <RichNarrative id="report-summary:body" className="report-summary-lead" value={`## Recommended labeling decision

Use three provenance classes: **CODE**, **HUMAN**, and **UNKNOWN**. Train the auditor-outcome model only on HUMAN rows established by an authenticated status-change event. Do not convert UNKNOWN into HUMAN: automatic success commonly produces status V with no reason, and code also writes I, P, MR, C, and N.

If historical status-change events do not exist, the repository can identify many automatic cases, but it cannot prove that all remaining cases were decided by an auditor.`} />
    </ReportSection>

    <DataComponent id="report-coverage" title="Repository validation surface" queryId="validation_coverage"
      kind="table" sourceRows={coverage} description="Static source scan; counts are not production-row volumes.">
      <DataTable rows={coverage} columns={coverageColumns} rowKey="measure" searchable={false} caption="Repository validation scan" />
    </DataComponent>

    <ReportSection id="report-interpretation" title="What status and reason can establish" queryId="status_assignment"
      sourceRows={statuses} showHeading={false}>
      <RichNarrative id="report-interpretation:body" value={`## What status and reason can establish

- A reason beginning with **VL**, **AU**, **DU**, or **SY** is strong evidence of a code rule when its exact normalized text maps to a Java constant or generated message.
- Status **V** with a null reason is not human evidence; many validators set it automatically.
- The fields approved, accepted_qe, review_qe, and fk_usr are not sufficient individually because application code also writes approval and review-related values.
- A status that does not match a known rule is **UNKNOWN**, not automatically HUMAN.`} />
    </ReportSection>

    <DataComponent id="report-status-table" title="Statuses directly assigned by Java code" queryId="status_assignment"
      kind="table" sourceRows={statuses} description="Static setter counts demonstrate code reachability, not runtime frequency.">
      <DataTable rows={statuses} columns={statusColumns} rowKey="status" searchable={false} caption="Code status assignments" />
    </DataComponent>

    <ReportSection id="report-classifier" title="Conservative historical classifier" queryId="status_assignment"
      sourceRows={statuses} showHeading={false}>
      <RichNarrative id="report-classifier:body" value={`## Conservative historical classifier

Build a versioned catalog of exact automatic reason patterns from the Java validators. Classify a row as CODE only when a known rule, system-generation marker, import marker, or execution event proves it. Classify a row as HUMAN only when status history or an application event proves an authenticated auditor submitted V or I. Everything else remains UNKNOWN.

For ML training, require decision_source = HUMAN, final_status in (V, I), a pre-decision feature snapshot, and an explicit manual-review routing event. Do not use final status, reason, approval, reviewer, accepted/review quarter, or post-decision values as features.`} />
    </ReportSection>

    <DataComponent id="report-aadhaar" title="Aadhaar validator examples" queryId="aadhaar_examples"
      kind="table" sourceRows={aadhaar} description="Reviewed implementations in UID and BIOMET.">
      <DataTable rows={aadhaar} columns={aadhaarColumns} rowKey="processor" searchable={false} caption="UID and BIOMET validation behavior" />
    </DataComponent>

    <ReportSection id="report-implementation" title="Implementation sequence" queryId="validation_coverage"
      sourceRows={coverage} showHeading={false}>
      <RichNarrative id="report-implementation:body" value={`## Implementation sequence

1. Extract validators into a version-controlled rule catalog containing processor, condition, normalized reason pattern, and resulting status.
2. Add append-only status history containing old/new status, decision source, rule code, actor, timestamp, and application version.
3. Capture a feature snapshot when a case is routed to manual review, before the auditor decides.
4. Backfill CODE only where deterministic evidence exists. Recover HUMAN from audit logs or reviewer events. Leave all other history UNKNOWN.
5. Train on proven HUMAN decisions and use a time-based, Army-number-grouped evaluation split.

### Critical limitation

Static source analysis describes expected behavior but cannot prove which branch executed for an individual row. Mutable reference data and code-version changes also make present-day replay unreliable for old records unless historical snapshots and application versions are available.`} />
    </ReportSection>
  </article>;
}
