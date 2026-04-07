from __future__ import annotations

import sys
from pathlib import Path


EXPECTED_WRAPPER_TEXT = """# Agent Policy Wrapper

Canonical policy is in `/AGENTS.md`.

This file must remain a thin pointer only.
Do not add operational rules here.
"""

REQUIRED_CANONICAL_SECTIONS = [
    "## Canonical Policy",
    "## Architecture Direction",
    "## CommandExecutor Guardrails",
    "## Bridge Mirroring Policy (`1C + 2B`)",
    "## Local-Only Bridge Policy",
    "## Motion Humanization Policy",
    "## Anti-Repeat Dispatch Policy",
    "## Scoring Protocol",
]


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    wrapper_path = repo_root / "AGENT.md"
    canonical_path = repo_root / "AGENTS.md"

    errors: list[str] = []

    if not canonical_path.exists():
        errors.append("Missing canonical policy file: AGENTS.md")
    if not wrapper_path.exists():
        errors.append("Missing wrapper file: AGENT.md")

    if canonical_path.exists():
        canonical_text = canonical_path.read_text(encoding="utf-8")
        for required in REQUIRED_CANONICAL_SECTIONS:
            if required not in canonical_text:
                errors.append(f"AGENTS.md missing required section: {required}")

    if wrapper_path.exists():
        wrapper_text = wrapper_path.read_text(encoding="utf-8")
        if wrapper_text.strip() != EXPECTED_WRAPPER_TEXT.strip():
            errors.append("AGENT.md must remain an exact thin wrapper pointing to /AGENTS.md")

    agent_files = sorted(p.name for p in repo_root.glob("AGENT*.md"))
    if agent_files != ["AGENT.md", "AGENTS.md"]:
        errors.append(
            "Unexpected AGENT*.md files found; expected only AGENT.md and AGENTS.md. "
            f"Found: {', '.join(agent_files) if agent_files else '(none)'}"
        )

    if errors:
        for error in errors:
            print(f"[agent-policy] ERROR: {error}")
        return 1

    print("[agent-policy] OK: canonical policy + wrapper are valid.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
