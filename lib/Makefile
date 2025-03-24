# Makefile for SysY library
# Cross-platform compatible version

# Detect OS
ifeq ($(OS),Windows_NT)
    DETECTED_OS := Windows
    # Check if using MinGW/MSYS
    ifeq ($(shell uname -s),MINGW)
        DETECTED_OS := MinGW
    endif
else
    DETECTED_OS := $(shell uname -s)
endif

# Set platform-specific variables
ifeq ($(DETECTED_OS),Windows)
    RM := del /Q
    TARGET := libsysy.a
    OBJ_EXT := obj
else
    RM := rm -f
    TARGET := libsysy.a
    OBJ_EXT := o
endif

# Compiler
CC := gcc

# Flags
CFLAGS := -Wall -O2

# Archive command
AR := ar

# Source files
SRC := sylib.c

# Object files
OBJS := $(SRC:.c=.$(OBJ_EXT))

.PHONY: all clean

all: $(TARGET)

$(TARGET): $(OBJS)
	$(AR) rcs $@ $^

%.$(OBJ_EXT): %.c
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	$(RM) $(OBJS) $(TARGET)

# Cross-compile versions (optional)
arm: clean
	arm-linux-gnueabihf-gcc $(CFLAGS) -c $(SRC) -o $(SRC:.c=.$(OBJ_EXT))
	arm-linux-gnueabihf-ar rcs $(TARGET) $(OBJS)

riscv: clean
	riscv64-unknown-elf-gcc $(CFLAGS) -c $(SRC) -o $(SRC:.c=.$(OBJ_EXT))
	riscv64-unknown-elf-ar rcs $(TARGET) $(OBJS)

# Print detected OS (for debugging)
os:
	@echo Detected OS: $(DETECTED_OS) 