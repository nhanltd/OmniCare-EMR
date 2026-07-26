#!/usr/bin/env python3
"""
OmniCare EMR - Root E2E Test Suite Launcher
Delegates to e2e-tests/e2e_test_suite.py for execution.
"""
import sys
import os

# Set working directory to project root and import runner
script_dir = os.path.dirname(os.path.abspath(__file__))
e2e_dir = os.path.join(script_dir, "e2e-tests")
sys.path.insert(0, e2e_dir)

from e2e_test_suite import main

if __name__ == "__main__":
    main()
