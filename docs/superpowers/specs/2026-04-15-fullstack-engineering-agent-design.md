---
title: Fullstack Engineering Agent Global Skill Design
date: 2026-04-15
status: approved-design
---

# Fullstack Engineering Agent Global Skill Design

## Goal

Create a personal global Skill named `fullstack-engineering-agent` that activates only when a user message starts with one of the supported slash commands. The Skill should provide a structured enterprise-style engineering workflow while still allowing the user to explicitly relax the process with phrases such as "directly do it" or "skip questions".

## Scope

This design covers:

- Personal global Skill placement, not project-local configuration
- Slash-command-triggered workflow routing
- Shared workflow rules across all supported commands
- Command-specific stage definitions
- Output constraints for design and code tasks
- Skip and risk-handling rules

This design does not cover:

- Skill installation packaging beyond the personal global Skill layout
- Automatic git commits
- Natural-language auto-triggering without slash commands

## Supported Commands

The Skill supports the following commands when they appear as the first token in the user message:

- `/req`
- `/sheji`
- `/tech`
- `/db`
- `/api`
- `/dev`
- `/batch`
- `/test`
- `/deploy`
- `/debug`

If the first token is not one of these commands, the Skill must not activate.

## Trigger Rules

### Activation Boundary

The Skill activates only when the message begins with a supported slash command. This keeps ordinary conversation and non-command requests outside the workflow.

### Non-Activation Cases

The Skill must not activate when:

- A slash command appears later in the message instead of at the beginning
- The user uses natural language without a supported slash command
- The command is unsupported or misspelled

### Unsupported Commands

If a message begins with an unsupported slash command, the Skill should not guess intent. It should state that the command is unsupported and list the supported commands.

## Core Operating Model

All supported commands share a four-layer control flow.

### 1. Command Entry

The agent identifies the command and echoes the active mode plus the stated goal. Example: entering `/dev` for a request to add validation to an order endpoint.

### 2. Information Check

The Skill asks clarifying questions before execution, but adapts the number of questions to the quality of the prompt:

- If the request is vague, ask 6-8 key questions
- If the request is already specific, ask only 1-3 critical follow-up questions
- If the user explicitly says to skip questioning, keep only the minimum necessary confirmation for risky or ambiguous work

This keeps the workflow structured without forcing a rigid question count in every case.

### 3. Stage-by-Stage Execution

Each command executes in ordered stages. At the end of each stage, the agent pauses and waits for user confirmation before continuing.

Allowed user confirmations include explicit continuations such as:

- "continue"
- "next stage"
- equivalent clear approval

### 4. Structured Outputs

Every stage must produce structured engineering-style output. The agent must not jump directly to a final answer when intermediate stages are still required.

## Skip Policy

The Skill is intentionally strong by default, but not absolute.

### Allowed Relaxation

If the user explicitly says any equivalent of the following, the Skill may relax the default process:

- "directly do it"
- "skip questions"
- "no need to wait for confirmation"
- "go to the next stage"

### Minimum Safety Floor

Even when the user relaxes the process, the Skill must still keep the minimum safeguards for risky work:

- Ask at least one key question if the task is materially ambiguous
- Refuse to fabricate code context for code modifications
- Preserve stage ordering where skipping would create invalid output

## Command-Specific Stages

### `/req`

Stages:

1. Requirement clarification
2. Requirement breakdown
3. PRD-style output

Expected content:

- User roles
- Use cases
- goals
- existing system context
- payment or permission constraints
- concurrency or scale assumptions
- timing or deadline requirements
- feature module breakdown
- MVP versus full-scope prioritization

### `/sheji`

Stages:

1. Supplemental questions
2. Three architecture options
3. Trade-off comparison
4. Final architecture proposal

Required option set:

- Monolith
- Layered architecture
- Microservices

Comparison dimensions:

- Performance
- Scalability
- Cost

### `/tech`

Stages:

1. Requirement confirmation
2. Technology comparison
3. Stack decision
4. Technical solution document

Required comparison families:

- Frontend: Vue / React / Mini Program as relevant
- Backend: Spring Boot / Node / Go as relevant
- Storage: MySQL / Redis / Elasticsearch as relevant

### `/db`

Stages:

1. Data and transaction clarification
2. Data modeling
3. Optimization
4. SQL output

Required content:

- ER relationships
- table structure
- indexing
- sharding or partitioning considerations where relevant

### `/api`

Stages:

1. Interface requirement confirmation
2. Interface definition
3. Security design
4. Swagger-style output

Required interface fields:

- URL
- method
- request
- response

### `/dev`

Stages:

1. Requirement confirmation
2. Context analysis
3. Code implementation
4. Impact analysis

The agent must inspect actual files before claiming a code change is complete.

### `/batch`

Stages:

1. Implicit requirement alignment
2. Modification plan
3. Multi-file change output
4. JSON change package

This mode is for coordinated multi-file edits and should include explicit file ordering.

### `/test`

Stages:

1. Test target confirmation
2. Test cases
3. Test strategy
4. Test plan output

Required case categories:

- Normal cases
- Exception cases
- Boundary cases

### `/deploy`

Stages:

1. Environment confirmation
2. Deployment architecture
3. CI/CD flow
4. Risk control

Required deployment coverage:

- Docker and/or Kubernetes as applicable

### `/debug`

Stages:

1. Information gathering
2. Root-cause hypotheses
3. Localization steps
4. Fix options

The Skill should require at least three plausible causes before narrowing to a fix.

## Code Output Standards

The Skill must enforce additional structure for `/dev` and `/batch`.

### `/dev` Output Skeleton

For code changes, the agent should use this structure when presenting implementation details:

- `【文件路径】`
- `【修改前】`
- `【修改后】`
- `【说明】`
- `【影响分析】`

Rules:

- Always identify the real file path
- Follow the minimal-change principle
- Include a diff summary if code has been changed
- If code has not been changed yet, remain in analysis mode instead of claiming completion

### `/batch` Output Skeleton

For batch changes, the agent should first present a modification plan, then present file-by-file changes, then provide a JSON change package.

Required JSON shape:

```json
{
  "changes": [
    {
      "file": "path/to/file",
      "before": "key snippet or diff context",
      "after": "key snippet or diff context"
    }
  ],
  "commit_message": "feat: short summary"
}
```

For large files, `before` and `after` may contain focused snippets or diff-style context instead of full file contents.

## Git and Traceability

The Skill should preserve change traceability, but should not automatically commit.

Required behavior:

- Provide a suggested commit message when relevant
- Keep modifications attributable to concrete file changes
- Avoid automatic git actions unless the user explicitly requests them

## Risk and Error Handling

### Insufficient Information

If the request lacks enough detail to produce valid output, the Skill must continue asking questions instead of guessing.

### Code Context Gaps

For code modification requests, the Skill must inspect related files and dependencies before presenting implementation as complete.

### Design Output Quality

For design-oriented commands, the Skill must compare multiple options and include trade-offs rather than presenting a single unexamined answer.

### Stage Integrity

The Skill should not skip required stages by default. Stages may be relaxed only when the user explicitly instructs it and when output validity is preserved.

## Skill Authoring Guidance

The future Skill should be authored with:

- A concise `description` that states only the trigger condition
- No workflow details in the `description`
- The detailed routing and behavior rules in `SKILL.md`

A suitable description pattern is:

`Use when a user message begins with /req, /sheji, /tech, /db, /api, /dev, /batch, /test, /deploy, or /debug and a structured engineering workflow should be applied.`

## Recommended Implementation Shape

The personal global Skill should contain:

- `SKILL.md` with routing rules and stage logic
- optional `agents/openai.yaml` if the runtime benefits from UI metadata

No additional files are required for the initial version.

## Acceptance Criteria

The design is successful if the implemented Skill does all of the following:

- Activates only on supported slash commands at the start of the message
- Does not affect ordinary non-command conversations
- Uses adaptive clarification instead of a fixed question count in every case
- Enforces stage-based responses with confirmation pauses by default
- Allows explicit user overrides for lower-friction execution
- Applies stronger structural requirements to `/dev` and `/batch`
- Maintains traceability without forcing git commits
