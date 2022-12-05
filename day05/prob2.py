# ChatGPT generated code with two manual fixes

def read_instruction():
  # Read an instruction from the console
  line = input()

  # Split the instruction into words
  words = line.split()

  # Extract the numbers from the instruction
  n = int(words[1])
  s1 = int(words[3])
  s2 = int(words[5])

  return n, s1, s2

def move(n, source, dest):
  # Pop the top n elements from the source stack
  elements = [source.pop() for _ in range(n)]

  # Push the popped elements onto the destination stack
  for element in reversed(elements):
    dest.append(element)

def main():
  # Define the stacks as lists
  stack1 = list(reversed(["Q", "H", "C", "T", "N", "S", "V", "B"]))
  stack2 = list(reversed(["G", "B", "D", "W"]))
  stack3 = list(reversed(["B", "Q", "S", "T", "R", "W", "F"]))
  stack4 = list(reversed(["N", "D", "J", "Z", "S", "W", "G", "L"]))
  stack5 = list(reversed(["F", "V", "D", "P", "M"]))
  stack6 = list(reversed(["J", "W", "F"]))
  stack7 = list(reversed(["V", "J", "B", "Q", "N", "L"]))
  stack8 = list(reversed(["N", "S", "Q", "J", "C", "R", "T", "G"]))
  stack9 = list(reversed(["M", "D", "W", "C", "Q", "S", "J"]))

  # Read instructions from the console until we reach the end of the input
  while True:
    try:
      # Read the next instruction from the console
      n, s1, s2 = read_instruction()

      # Perform the "move" operation on the stacks
      move(n, eval(f"stack{s1}"), eval(f"stack{s2}"))
    except EOFError:
      # Stop reading instructions when we reach the end of the input
      break

  # Print the final state of the stacks
  print(stack1)
  print(stack2)
  print(stack3)
  print(stack4)
  print(stack5)
  print(stack6)
  print(stack7)
  print(stack8)
  print(stack9)

main()

