#!/bin/env python3

from collections.abc import Iterator
from heapq import heappush, heappop
from typing import List, Tuple

import fileinput
import sys

def neighbors(y: int, x: int, height: int, width: int) -> Iterator[Tuple[int, int]]:
  if (y > 0):
    yield y - 1, x
  if (x > 0):
    yield y, x - 1
  if (y < height - 1):
    yield y + 1, x
  if (x < width - 1):
    yield y, x + 1


def dijkstra(height_map: List[List[int]], startX: int, startY: int, endX: int, endY: int) -> int | None:
  w, h = len(height_map[0]), len(height_map)
  # Python 3's int is unbounded, but sys.maxsize will serve our purpose for this input range
  cost_map = [[sys.maxsize for x in range(w)] for y in range(h)]
  cost_map[startY][startX] = 0

  visited = set()
  pq = []
  heappush(pq, (0, (startY, startX)))

  while len(pq) > 0:
    (cost, (y, x)) = heappop(pq)
    visited.add((y, x))

    for ny, nx in neighbors(y, x, h, w):
      if (ny, nx) not in visited and height_map[ny][nx] - height_map[y][x] <= 1:
        old_cost = cost_map[ny][nx]
        # steps
        new_cost = cost_map[y][x] + 1
        if new_cost < old_cost:
          heappush(pq, (new_cost, (ny, nx)))
          cost_map[ny][nx] = new_cost
          if ny == endY and nx == endX:
            return new_cost
  
  return None


if __name__ == "__main__":
  raw_map = [line.strip() for line in fileinput.input()]

  height_map = [[ord(n) - ord('a') for n in line] for line in raw_map]
  
  startX, startY = 0, 0
  endX, endY = 0, 0
  for y, row in enumerate(raw_map):
    for x, val in enumerate(row):
      if val == 'S':
        startX, startY = x, y
        height_map[y][x] = 0
      elif val == 'E':
        endX, endY = x, y
        height_map[y][x] = ord('z') - ord('a')

  print(f"Start: {startX}, {startY}; end: {endX}, {endY}")
  steps = dijkstra(height_map, startX, startY, endX, endY)

  if steps is not None:
    print(f"Best signal reached after {steps} steps.")

    all_start_points_steps: List[Tuple[int, Tuple[int, int]]] = []
    # Apparently this may be optimized, for example all starting points that have been visited
    # in an unsuccessful search should be excluded.
    for y, row in enumerate(height_map):
      for x, height in enumerate(row):
        if height == 0:
          this_steps = dijkstra(height_map, x, y, endX, endY)
          if this_steps is not None:
            all_start_points_steps.append((this_steps, (y, x)))

    all_start_points_steps.sort()
    fewest = all_start_points_steps[0]
    print(f"Fewest steps {fewest[0]} starting from {fewest[1][0]}, {fewest[1][1]}")
