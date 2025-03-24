#!/usr/bin/env python3
"""
SysY Compiler Testing Framework
This script runs tests for the SysY compiler to verify the correctness
of compilation across different targets and optimization levels.
"""

import os
import re
import sys
import glob
import json
import argparse
import subprocess
import tempfile
import shutil
import time
import platform
import logging
from concurrent.futures import ThreadPoolExecutor
from enum import Enum
from pathlib import Path

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    datefmt="%H:%M:%S"
)

# Detect operating system
IS_WINDOWS = platform.system() == "Windows"
IS_MACOS = platform.system() == "Darwin"
IS_LINUX = platform.system() == "Linux"

# Set up paths
SCRIPT_DIR = Path(os.path.dirname(os.path.abspath(__file__)))
TEST_ROOT = SCRIPT_DIR.parent
PROJECT_ROOT = TEST_ROOT.parent

# Default paths
DEFAULT_COMPILER_JAR = PROJECT_ROOT / "target" / "compiler-1.0-SNAPSHOT-jar-with-dependencies.jar"
CASES_DIR = TEST_ROOT / "cases"
EXPECTED_DIR = TEST_ROOT / "expected"
EMULATORS_DIR = TEST_ROOT / "emulators"
LIB_DIR = PROJECT_ROOT / "lib"
LIBSYSY_A = LIB_DIR / "libsysy.a"
SYLIB_H = LIB_DIR / "sylib.h"

# Configuration file path
CONFIG_FILE = TEST_ROOT / "config.json"

# Executable extension
EXE_EXT = ".exe" if IS_WINDOWS else ""

# Default configuration
DEFAULT_CONFIG = {
    "paths": {
        "compiler_jar": os.path.join(PROJECT_ROOT, "target", "compiler-1.0-SNAPSHOT-jar-with-dependencies.jar"),
        "arm_cc": "arm-linux-gnueabihf-gcc",
        "riscv_cc": "riscv64-unknown-elf-gcc",
        "qemu_arm": "qemu-arm",
        "qemu_riscv": "qemu-riscv64"
    },
    "options": {
        "default_timeout": 30,
        "default_target": "arm",
        "default_optimization": "O0",
        "targets": ["arm", "riscv"],
        "optimizations": ["O0", "O1", "O2"]
    }
}

# Override with platform-specific defaults if needed
if IS_MACOS:
    DEFAULT_CONFIG["paths"]["arm_cc"] = "arm-none-eabi-gcc"
elif IS_WINDOWS:
    DEFAULT_CONFIG["paths"]["arm_cc"] += ".exe"
    DEFAULT_CONFIG["paths"]["riscv_cc"] += ".exe"
    DEFAULT_CONFIG["paths"]["qemu_arm"] += ".exe"
    DEFAULT_CONFIG["paths"]["qemu_riscv"] += ".exe"

# Load or create configuration
def load_config():
    """Load or create configuration"""
    config = DEFAULT_CONFIG.copy()
    if os.path.exists(CONFIG_FILE):
        try:
            with open(CONFIG_FILE, 'r') as f:
                loaded_config = json.load(f)
                logging.info(f"Loaded configuration from {CONFIG_FILE}")
                
                # Merge paths
                if "paths" in loaded_config:
                    config["paths"].update(loaded_config["paths"])
                
                # Merge options
                if "options" in loaded_config:
                    config["options"].update(loaded_config["options"])
                
                # Handle compiler_jar path format
                if "compiler_jar" in config["paths"]:
                    current_os_path = str(DEFAULT_COMPILER_JAR).replace('\\', '/')
                    if (IS_WINDOWS and '/' in config["paths"]["compiler_jar"] and not '\\' in config["paths"]["compiler_jar"]) or \
                       (not IS_WINDOWS and '\\' in config["paths"]["compiler_jar"]):
                        logging.warning(f"Detected mismatched path format in config. Updating compiler_jar path.")
                        config["paths"]["compiler_jar"] = current_os_path
                
                # Fix executable extensions
                if IS_LINUX or IS_MACOS:
                    for key in ["arm_cc", "riscv_cc", "qemu_arm", "qemu_riscv"]:
                        if key in config["paths"] and config["paths"][key].endswith(".exe"):
                            config["paths"][key] = config["paths"][key][:-4]
                            logging.warning(f"Removed .exe suffix from {key} path")
                elif IS_WINDOWS:
                    for key in ["arm_cc", "riscv_cc", "qemu_arm", "qemu_riscv"]:
                        if key in config["paths"] and not config["paths"][key].endswith(".exe"):
                            config["paths"][key] += ".exe"
                            logging.warning(f"Added .exe suffix to {key} path")
        except Exception as e:
            logging.warning(f"Failed to load config file: {e}")
            config = DEFAULT_CONFIG.copy()
    
    # Auto-detect RISC-V toolchain if needed
    if not os.path.exists(config["paths"]["riscv_cc"]) and not shutil.which(config["paths"]["riscv_cc"]):
        logging.info("RISC-V toolchain not found, attempting auto-detection")
        riscv_gcc_names = [
            "riscv64-unknown-elf-gcc",
            "riscv64-linux-gnu-gcc",
            "riscv64-elf-gcc",
            "riscv-none-embed-gcc",
            "riscv-linux-gnu-gcc",
            "riscv-none-elf-gcc",
            "riscv64-unknown-linux-gnu-gcc"
        ]
        
        for gcc_name in riscv_gcc_names:
            gcc_path = shutil.which(gcc_name)
            if gcc_path:
                config["paths"]["riscv_cc"] = gcc_name
                logging.info(f"Found RISC-V toolchain: {gcc_name}")
                break
    
    # Save updated configuration
    try:
        with open(CONFIG_FILE, 'w') as f:
            json.dump(config, f, indent=4)
            logging.info(f"Updated configuration at {CONFIG_FILE}")
    except Exception as e:
        logging.warning(f"Failed to write config: {e}")
    
    return config

# Load configuration
CONFIG = load_config()

class TestResult(Enum):
    PASS = "PASS"
    FAIL = "FAIL"
    ERROR = "ERROR"
    TIMEOUT = "TIMEOUT"
    SKIPPED = "SKIPPED"

class TestCase:
    def __init__(self, file_path):
        self.file_path = file_path
        self.name = os.path.basename(file_path)
        self.category = os.path.basename(os.path.dirname(file_path))
        self.config = self._parse_config()
        self.expected_output = self._load_expected_output()
    
    def _parse_config(self):
        """Parse test configuration from comments in source file"""
        config = {
            "targets": CONFIG["options"]["targets"],
            "optimizations": CONFIG["options"]["optimizations"],
            "timeout": CONFIG["options"]["default_timeout"]
        }
        
        try:
            with open(self.file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                match = re.search(r'// TEST_CONFIG:\s*(.*?)(?:\n|$)', content)
                if match:
                    config_str = match.group(1)
                    for item in config_str.split(';'):
                        if '=' in item:
                            key, value = item.split('=', 1)
                            key = key.strip()
                            value = [v.strip() for v in value.split(',')]
                            if key == 'target':
                                config['targets'] = value
                            elif key == 'optimization':
                                config['optimizations'] = value
                            elif key == 'timeout':
                                config['timeout'] = int(value[0])
        except Exception as e:
            logging.warning(f"Error parsing test config for {self.name}: {e}")
        
        return config
    
    def _load_expected_output(self):
        """Load expected output for the test case"""
        base_name = os.path.splitext(self.name)[0]
        expected_file = EXPECTED_DIR / f"{base_name}.out"
        
        if expected_file.exists():
            try:
                with open(expected_file, 'r', encoding='utf-8') as f:
                    return f.read().strip()
            except Exception as e:
                logging.warning(f"Error reading expected output for {self.name}: {e}")
                return ""
        return ""
    
    def __str__(self):
        return f"{self.category}/{self.name}"

def collect_test_cases(args):
    """Collect test cases based on command-line arguments"""
    if args.file:
        # Single file test
        file_path = Path(args.file)
        if file_path.exists():
            return [TestCase(str(file_path))]
        else:
            logging.error(f"Test file not found: {args.file}")
            return []
    
    categories = [args.category] if args.category else ["syntax", "semantic", "optimization", "functional"]
    
    test_cases = []
    for category in categories:
        category_dir = CASES_DIR / category
        if category_dir.exists():
            for file_path in glob.glob(str(category_dir / "*.sy")):
                test_cases.append(TestCase(file_path))
        else:
            logging.warning(f"Category directory not found: {category_dir}")
    
    logging.info(f"Collected {len(test_cases)} test cases")
    return test_cases

def check_compiler_jar(compiler_jar):
    """Check if the compiler JAR exists and is valid"""
    # 尝试不同路径格式，以处理跨平台问题
    paths_to_check = [
        compiler_jar,
        compiler_jar.replace('\\', '/'),
        compiler_jar.replace('/', '\\')
    ]
    
    for path in paths_to_check:
        if os.path.exists(path):
            if path != compiler_jar:
                logging.warning(f"Found compiler at alternative path: {path}")
                # 更新配置中的路径
                CONFIG["paths"]["compiler_jar"] = path
                try:
                    with open(CONFIG_FILE, 'w') as f:
                        json.dump(CONFIG, f, indent=4)
                    logging.info(f"Updated compiler_jar path in configuration")
                except Exception as e:
                    logging.warning(f"Failed to update config file: {e}")
            return True
    
    # 尝试在当前目录结构中查找
    alternative_path = str(PROJECT_ROOT / "target" / "compiler-1.0-SNAPSHOT-jar-with-dependencies.jar")
    if os.path.exists(alternative_path):
        logging.info(f"Found compiler at alternative path: {alternative_path}")
        CONFIG["paths"]["compiler_jar"] = alternative_path
        try:
            with open(CONFIG_FILE, 'w') as f:
                json.dump(CONFIG, f, indent=4)
            logging.info(f"Updated compiler_jar path in configuration")
        except Exception as e:
            logging.warning(f"Failed to update config file: {e}")
        return True
    
    logging.error(f"Compiler JAR not found: {compiler_jar}")
    logging.error("Please build the compiler or update the config.json with the correct path")
    logging.error(f"Expected location: {PROJECT_ROOT}/target/compiler-1.0-SNAPSHOT-jar-with-dependencies.jar")
    return False

def check_cross_compiler(compiler_name, compiler_path):
    """Check if a cross-compiler is available"""
    try:
        result = subprocess.run(
            [compiler_path, "--version"], 
            stdout=subprocess.PIPE, 
            stderr=subprocess.PIPE,
            timeout=5,
            text=True
        )
        if result.returncode == 0:
            return True
    except FileNotFoundError:
        logging.error(f"{compiler_name} not found at: {compiler_path}")
        return False
    except subprocess.TimeoutExpired:
        logging.error(f"Timeout checking {compiler_name}")
        return False
    except Exception as e:
        logging.error(f"Error checking {compiler_name}: {e}")
        return False
    
    return False

def compile_test(test_case, target, optimization, verbose=False):
    """Compile a test case with specific options"""
    compiler_jar = CONFIG["paths"]["compiler_jar"]
    
    if not check_compiler_jar(compiler_jar):
        return False, None, "Compiler JAR not found"
    
    output_file = tempfile.mktemp(suffix=".s")
    
    cmd = [
        "java", "-jar", compiler_jar,
        test_case.file_path,
        "-S",
        f"--target", target,
        f"-{optimization}",
        "-o", output_file
    ]
    
    if verbose:
        logging.info(f"Executing: {' '.join(cmd)}")
    
    try:
        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=test_case.config["timeout"],
            text=True
        )
        
        success = result.returncode == 0
        
        if verbose and not success:
            logging.error(f"Compilation failed: {result.stderr}")
        
        # 检查输出的汇编文件是否存在并有效
        if success and (not os.path.exists(output_file) or os.path.getsize(output_file) == 0):
            success = False
            error_msg = "Compilation succeeded but did not generate a valid assembly file"
            if verbose:
                logging.error(error_msg)
            return False, None, error_msg
        
        return success, output_file, result.stderr
    except subprocess.TimeoutExpired:
        if verbose:
            logging.warning(f"Compilation timeout after {test_case.config['timeout']} seconds")
        
        return False, None, "Compilation timeout"
    except Exception as e:
        if verbose:
            logging.error(f"Compilation error: {e}")
        
        return False, None, f"Compilation error: {str(e)}"

def run_arm_test(assembly_file, timeout, verbose=False):
    """Run a compiled ARM assembly file using QEMU and collect output"""
    arm_cc = CONFIG["paths"]["arm_cc"]
    qemu_arm = CONFIG["paths"]["qemu_arm"]
    
    # 检查ARM编译器是否可用
    if not os.path.exists(arm_cc) and not shutil.which(arm_cc):
        error_msg = f"ARM compiler not found: {arm_cc}"
        if verbose:
            logging.error(error_msg)
        return False, error_msg
    
    # 检查QEMU是否可用
    if not os.path.exists(qemu_arm) and not shutil.which(qemu_arm):
        error_msg = f"QEMU for ARM not found: {qemu_arm}"
        if verbose:
            logging.error(error_msg)
        return False, error_msg
    
    # Create a C wrapper to call our assembly
    wrapper_c = tempfile.mktemp(suffix=".c")
    with open(wrapper_c, 'w') as f:
        f.write("""
#include <stdio.h>
#include <stdlib.h>

/* 使用唯一名称避免与汇编中的main冲突 */
extern int main();

/* 改为唯一的包装器名称 */
int sysy_test_wrapper() {
    int result = main();
    printf("Exit code: %d\\n", result);
    return result;
}

/* 重命名main函数避免冲突 */
int main() {
    return sysy_test_wrapper();
}
""")
    
    output_exe = tempfile.mktemp(suffix=EXE_EXT)
    
    try:
        # Copy sylib.h to temp location for compilation
        temp_sylib_h = os.path.join(os.path.dirname(wrapper_c), "sylib.h")
        shutil.copyfile(str(SYLIB_H), temp_sylib_h)
        
        # Compile with wrapper and link with libsysy.a - 使用-Wl,--allow-multiple-definition允许重复定义
        compile_cmd = [arm_cc, "-static", wrapper_c, assembly_file, str(LIBSYSY_A), "-o", output_exe, 
                      "-I", os.path.dirname(temp_sylib_h), "-Wl,--allow-multiple-definition"]
        if verbose:
            logging.info(f"Executing: {' '.join(compile_cmd)}")
        
        compile_result = subprocess.run(
            compile_cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
            text=True
        )
        
        if compile_result.returncode != 0:
            if verbose:
                logging.error(f"GCC compilation failed: {compile_result.stderr}")
            return False, "GCC compilation failed: " + compile_result.stderr
        
        # Run with QEMU
        run_cmd = [qemu_arm, output_exe]
        if verbose:
            logging.info(f"Executing: {' '.join(run_cmd)}")
        
        run_result = subprocess.run(
            run_cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
            text=True
        )
        
        return True, run_result.stdout.strip()
    
    except subprocess.TimeoutExpired:
        if verbose:
            logging.warning(f"Execution timeout after {timeout} seconds")
        return False, "Execution timeout"
    except FileNotFoundError as e:
        if verbose:
            logging.error(f"Command not found: {e}")
        return False, f"Command not found: {e}"
    except Exception as e:
        if verbose:
            logging.error(f"Execution error: {str(e)}")
        return False, f"Execution error: {str(e)}"
    finally:
        # Clean up
        for temp_file in [wrapper_c, output_exe, temp_sylib_h]:
            if os.path.exists(temp_file):
                try:
                    os.remove(temp_file)
                except:
                    pass

def run_riscv_test(assembly_file, timeout, verbose=False):
    """Run a compiled RISC-V assembly file using QEMU and collect output"""
    riscv_cc = CONFIG["paths"]["riscv_cc"]
    qemu_riscv = CONFIG["paths"]["qemu_riscv"]
    
    # 检查RISC-V编译器是否可用
    if not os.path.exists(riscv_cc) and not shutil.which(riscv_cc):
        error_msg = f"RISC-V compiler not found: {riscv_cc}"
        if verbose:
            logging.error(error_msg)
        return False, error_msg
    
    # 检查QEMU是否可用
    if not os.path.exists(qemu_riscv) and not shutil.which(qemu_riscv):
        error_msg = f"QEMU for RISC-V not found: {qemu_riscv}"
        if verbose:
            logging.error(error_msg)
        return False, error_msg
    
    # Create a C wrapper to call our assembly
    wrapper_c = tempfile.mktemp(suffix=".c")
    with open(wrapper_c, 'w') as f:
        f.write("""
#include <stdio.h>
#include <stdlib.h>

/* 使用唯一名称避免与汇编中的main冲突 */
extern int main();

/* 改为唯一的包装器名称 */
int sysy_test_wrapper() {
    int result = main();
    printf("Exit code: %d\\n", result);
    return result;
}

/* 重命名main函数避免冲突 */
int main() {
    return sysy_test_wrapper();
}
""")
    
    output_exe = tempfile.mktemp(suffix=EXE_EXT)
    
    try:
        # Copy sylib.h to temp location for compilation
        temp_sylib_h = os.path.join(os.path.dirname(wrapper_c), "sylib.h")
        shutil.copyfile(str(SYLIB_H), temp_sylib_h)
        
        # Compile with wrapper and link with libsysy.a - 使用-Wl,--allow-multiple-definition允许重复定义
        compile_cmd = [riscv_cc, "-static", wrapper_c, assembly_file, str(LIBSYSY_A), "-o", output_exe, 
                      "-I", os.path.dirname(temp_sylib_h), "-Wl,--allow-multiple-definition"]
        if verbose:
            logging.info(f"Executing: {' '.join(compile_cmd)}")
        
        compile_result = subprocess.run(
            compile_cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
            text=True
        )
        
        if compile_result.returncode != 0:
            if verbose:
                logging.error(f"GCC compilation failed: {compile_result.stderr}")
            return False, "GCC compilation failed: " + compile_result.stderr
        
        # Run with QEMU
        run_cmd = [qemu_riscv, output_exe]
        if verbose:
            logging.info(f"Executing: {' '.join(run_cmd)}")
        
        run_result = subprocess.run(
            run_cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
            text=True
        )
        
        return True, run_result.stdout.strip()
    
    except subprocess.TimeoutExpired:
        if verbose:
            logging.warning(f"Execution timeout after {timeout} seconds")
        return False, "Execution timeout"
    except FileNotFoundError as e:
        if verbose:
            logging.error(f"Command not found: {e}")
        return False, f"Command not found: {e}"
    except Exception as e:
        if verbose:
            logging.error(f"Execution error: {str(e)}")
        return False, f"Execution error: {str(e)}"
    finally:
        # Clean up
        for temp_file in [wrapper_c, output_exe, temp_sylib_h]:
            if os.path.exists(temp_file):
                try:
                    os.remove(temp_file)
                except:
                    pass

def run_test(test_case, target, optimization, verbose=False):
    """Run a test with specific configuration"""
    if target not in test_case.config["targets"] or optimization not in test_case.config["optimizations"]:
        return TestResult.SKIPPED, f"Test not configured for {target}/{optimization}", None
    
    # Compile the test case
    success, assembly_file, error_message = compile_test(test_case, target, optimization, verbose)
    if not success:
        return TestResult.ERROR, error_message, None
    
    try:
        # Run the compiled assembly
        if target == "arm":
            success, output = run_arm_test(assembly_file, test_case.config["timeout"], verbose)
        elif target == "riscv":
            success, output = run_riscv_test(assembly_file, test_case.config["timeout"], verbose)
        else:
            return TestResult.ERROR, f"Unsupported target: {target}", None
        
        if not success:
            return TestResult.ERROR, output, None
        
        # Compare with expected output
        success = test_case.expected_output in output
        if success:
            return TestResult.PASS, "Output matches expected", output
        else:
            diff_message = f"Expected: '{test_case.expected_output}'\nActual: '{output}'"
            return TestResult.FAIL, diff_message, output
    
    finally:
        # Clean up assembly file
        if assembly_file and os.path.exists(assembly_file):
            try:
                os.remove(assembly_file)
            except:
                pass

def print_result(test_case, target, optimization, result, message, verbose=False):
    """Print test result with colorization if available"""
    result_str = result.value
    
    # Add color if running in terminal
    if sys.stdout.isatty():
        if result == TestResult.PASS:
            result_str = f"\033[92m{result_str}\033[0m"  # Green
        elif result == TestResult.FAIL:
            result_str = f"\033[91m{result_str}\033[0m"  # Red
        elif result == TestResult.ERROR:
            result_str = f"\033[93m{result_str}\033[0m"  # Yellow
        elif result == TestResult.TIMEOUT:
            result_str = f"\033[95m{result_str}\033[0m"  # Purple
        elif result == TestResult.SKIPPED:
            result_str = f"\033[94m{result_str}\033[0m"  # Blue
    
    print(f"{test_case} [{target}] [{optimization}]: {result_str}")
    
    if verbose or result in [TestResult.FAIL, TestResult.ERROR]:
        print(f"  {message}")

def run_tests(test_cases, args):
    """Run all tests with the specified configurations"""
    results = {
        TestResult.PASS: 0,
        TestResult.FAIL: 0,
        TestResult.ERROR: 0,
        TestResult.TIMEOUT: 0,
        TestResult.SKIPPED: 0
    }
    
    targets = [args.target] if args.target else CONFIG["options"]["targets"]
    optimizations = [args.optimization] if args.optimization else CONFIG["options"]["optimizations"]

    # Check if compiler JAR exists
    if not check_compiler_jar(CONFIG["paths"]["compiler_jar"]):
        return

    print(f"Running {len(test_cases)} tests with targets={targets}, optimizations={optimizations}")
    
    for test_case in test_cases:
        for target in targets:
            for optimization in optimizations:
                result, message, output = run_test(test_case, target, optimization, args.verbose)
                print_result(test_case, target, optimization, result, message, args.verbose)
                results[result] += 1
    
    # Print summary
    total = sum(results.values())
    print("\nTest Summary:")
    print(f"Total: {total}")
    if total > 0:
        print(f"Passed: {results[TestResult.PASS]} ({results[TestResult.PASS]/total*100:.1f}%)")
        print(f"Failed: {results[TestResult.FAIL]} ({results[TestResult.FAIL]/total*100:.1f}%)")
        print(f"Errors: {results[TestResult.ERROR]} ({results[TestResult.ERROR]/total*100:.1f}%)")
        print(f"Timeouts: {results[TestResult.TIMEOUT]} ({results[TestResult.TIMEOUT]/total*100:.1f}%)")
        print(f"Skipped: {results[TestResult.SKIPPED]} ({results[TestResult.SKIPPED]/total*100:.1f}%)")
    else:
        print("No tests were run. Make sure your configuration is correct and test files exist.")

def main():
    """Main entry point for the test framework"""
    parser = argparse.ArgumentParser(description="SysY Compiler Testing Framework")
    parser.add_argument("--file", help="Run tests for a specific file")
    parser.add_argument("--category", help="Run tests for a specific category (syntax, semantic, optimization, functional)")
    parser.add_argument("--target", help="Target architecture (arm, riscv)")
    parser.add_argument("--optimization", help="Optimization level (O0, O1, O2)")
    parser.add_argument("--verbose", "-v", action="store_true", help="Enable verbose output")
    parser.add_argument("--update-config", action="store_true", help="Reset the configuration file to defaults")
    
    args = parser.parse_args()
    
    if args.update_config:
        config = DEFAULT_CONFIG.copy()
        
        # 确保compiler_jar使用当前系统格式的路径
        if IS_WINDOWS:
            config["paths"]["compiler_jar"] = str(DEFAULT_COMPILER_JAR).replace('/', '\\')
        else:
            config["paths"]["compiler_jar"] = str(DEFAULT_COMPILER_JAR).replace('\\', '/')
        
        # 修改为当前系统上的实际路径
        actual_jar_path = str(PROJECT_ROOT / "target" / "compiler-1.0-SNAPSHOT-jar-with-dependencies.jar")
        if os.path.exists(actual_jar_path):
            if IS_WINDOWS:
                config["paths"]["compiler_jar"] = actual_jar_path.replace('/', '\\')
            else:
                config["paths"]["compiler_jar"] = actual_jar_path.replace('\\', '/')
            print(f"Found compiler JAR at: {actual_jar_path}")
        else:
            print(f"Warning: Compiler JAR not found at: {actual_jar_path}")
            print("You need to build the compiler or manually update the path in config.json")
        
        # 确保编译器路径正确（添加或移除.exe）
        if IS_LINUX or IS_MACOS:
            for key in ["arm_cc", "riscv_cc", "qemu_arm", "qemu_riscv"]:
                if config["paths"][key].endswith(".exe"):
                    config["paths"][key] = config["paths"][key][:-4]
        elif IS_WINDOWS:
            for key in ["arm_cc", "riscv_cc", "qemu_arm", "qemu_riscv"]:
                if not config["paths"][key].endswith(".exe"):
                    config["paths"][key] += ".exe"
        
        # 检测QEMU安装情况
        print("Detecting QEMU...")
        qemu_executables = {
            "qemu_arm": ["qemu-arm", "qemu-system-arm"],
            "qemu_riscv": ["qemu-riscv64", "qemu-system-riscv64"]
        }
        
        for qemu_key, possible_names in qemu_executables.items():
            qemu_found = False
            for name in possible_names:
                if shutil.which(name):
                    config["paths"][qemu_key] = name
                    print(f"Found {qemu_key.replace('_', '-')}: {name}")
                    qemu_found = True
                    break
            
            if not qemu_found:
                print(f"Warning: {qemu_key.replace('_', '-')} not found in PATH.")
                if IS_LINUX:
                    print("You might need to install it with:")
                    print("  sudo apt-get install -y qemu-user qemu-system")
                elif IS_MACOS:
                    print("You might need to install it with:")
                    print("  brew install qemu")
        
        # 检测适合当前系统的ARM编译器
        if IS_LINUX:
            arm_compilers = ["arm-linux-gnueabihf-gcc", "arm-linux-gnu-gcc", "arm-none-eabi-gcc"]
            for compiler in arm_compilers:
                if shutil.which(compiler):
                    config["paths"]["arm_cc"] = compiler
                    print(f"Found ARM compiler: {compiler}")
                    break
        elif IS_MACOS:
            if shutil.which("arm-none-eabi-gcc"):
                config["paths"]["arm_cc"] = "arm-none-eabi-gcc"
                print("Found ARM compiler: arm-none-eabi-gcc")
        
        # Try to detect RISC-V toolchain
        print("Auto-detecting RISC-V toolchain...")
        riscv_gcc_names = [
            "riscv64-unknown-elf-gcc",
            "riscv64-linux-gnu-gcc",
            "riscv64-elf-gcc",
            "riscv-none-embed-gcc",
            "riscv-linux-gnu-gcc",
            "riscv-none-elf-gcc",
            "riscv64-unknown-linux-gnu-gcc"
        ]
        
        riscv_gcc_found = False
        for gcc_name in riscv_gcc_names:
            gcc_path = shutil.which(gcc_name)
            if gcc_path:
                config["paths"]["riscv_cc"] = gcc_name
                print(f"Found RISC-V toolchain: {gcc_name}")
                riscv_gcc_found = True
                break
        
        if not riscv_gcc_found:
            print("Warning: RISC-V toolchain not found in PATH.")
            if IS_LINUX:
                print("You might need to install it with:")
                print("  sudo apt-get install -y gcc-riscv64-unknown-elf")
                print("  # 或者尝试: sudo apt-get install -y gcc-riscv64-linux-gnu")
            elif IS_MACOS:
                print("You might need to install it with:")
                print("  brew tap riscv/riscv")
                print("  brew install riscv-gnu-toolchain")
            print("Or update config.json manually with the correct path.")
        
        with open(CONFIG_FILE, 'w') as f:
            json.dump(config, f, indent=4)
        print(f"Configuration updated at {CONFIG_FILE}")
        return
    
    test_cases = collect_test_cases(args)
    if not test_cases:
        print("No test cases found.")
        return
    
    run_tests(test_cases, args)

if __name__ == "__main__":
    main() 