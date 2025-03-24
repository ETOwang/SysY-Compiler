#!/usr/bin/env python3
"""
Helper script for creating new SysY compiler test cases.
"""

import os
import sys
import argparse
import textwrap

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
TEST_ROOT = os.path.dirname(SCRIPT_DIR)
CASES_DIR = os.path.join(TEST_ROOT, "cases")
EXPECTED_DIR = os.path.join(TEST_ROOT, "expected")

def create_test_case(category, name, description, target="arm,riscv", optimization="O0,O1,O2"):
    """Create a new test case with the specified parameters"""
    # Ensure directories exist
    category_dir = os.path.join(CASES_DIR, category)
    os.makedirs(category_dir, exist_ok=True)
    os.makedirs(EXPECTED_DIR, exist_ok=True)
    
    # Create SysY source file
    source_file = os.path.join(category_dir, f"{name}.sy")
    with open(source_file, 'w') as f:
        f.write(textwrap.dedent(f"""\
            // {description}
            // TEST_CONFIG: target={target}; optimization={optimization}
            
            int main() {{
                // TODO: Implement test case
                return 0;
            }}
        """))
    
    # Create expected output file
    expected_file = os.path.join(EXPECTED_DIR, f"{name}.out")
    with open(expected_file, 'w') as f:
        f.write("Exit code: 0\n")
    
    print(f"Created test case: {source_file}")
    print(f"Expected output: {expected_file}")
    print(f"Edit these files to implement your test.")

def main():
    parser = argparse.ArgumentParser(description="Create a new SysY compiler test case")
    parser.add_argument("category", choices=["syntax", "semantic", "optimization", "functional"], 
                        help="Test category")
    parser.add_argument("name", help="Test case name (without .sy extension)")
    parser.add_argument("--description", "-d", default="SysY compiler test case", 
                        help="Short description of the test")
    parser.add_argument("--target", default="arm,riscv", 
                        help="Target architectures to test, comma-separated")
    parser.add_argument("--optimization", default="O0,O1,O2", 
                        help="Optimization levels to test, comma-separated")
    
    args = parser.parse_args()
    
    create_test_case(args.category, args.name, args.description, args.target, args.optimization)
    
    return 0

if __name__ == "__main__":
    sys.exit(main()) 