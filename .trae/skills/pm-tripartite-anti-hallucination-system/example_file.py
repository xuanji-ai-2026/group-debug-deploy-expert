---
id: "EXAMPLE-001"
status: "dev"
role_owner: "developer"
version: 1.0
genesis_hash: ""
previous_hash: ""
last_updated: "2026-05-31T12:00:00Z"
changelog:
  - "v1.0: Initial version created @dispatcher"
tags: ["example", "template"]
---

# Example File - Standard Frontmatter Template

"""
This is an example file demonstrating how to use the standard YAML Frontmatter
format specified in the "AI Collaboration System Ultimate Constitution".
"""

## File Description

Every managed file must include a standard YAML Frontmatter at the beginning with the following required fields:

- **id**: Globally unique identifier
- **status**: Lifecycle status (dev/audit/accept/verified/blocked)
- **role_owner**: Current responsible role
- **version**: Version number (auto-incremented by 0.1 on each modification)
- **genesis_hash**: Initial hash at creation time
- **previous_hash**: Previous content hash (for tamper-proof verification)
- **last_updated**: Last update timestamp (ISO8601 format)
- **changelog**: Change log list
- **tags**: Semantic tags

## Usage Example

```python
# [MOD-20260531] @developer: Add example function
def example_function():
    """
    This is an example function demonstrating the use of living comments.
    """
    return "Hello, Tripartite System!"
```

## State Transition Rules

File status must follow this transition order:

```
dev -> audit -> accept -> verified
  |         |        |
blocked   dev      dev
```

**Cross-level jumps are PROHIBITED!**

## Notes

1. Must output RCA report before modifying code
2. All modifications must use micro-surgery (replace_in_file)
3. New code must include living comments
4. Keep changelog updated synchronously
