import sys

if __name__ == "__main__":
    total = 0
    for line in sys.stdin.readlines():
        half_size = len(line) // 2
        first_half = set(line[0:half_size])
        second_half = set(line[half_size:])

        dup = first_half.intersection(second_half).pop()
        priority = ord(dup) - 96 if dup >= 'a' else ord(dup) - 38
        total = total + priority
        print(priority)
    print(total)
