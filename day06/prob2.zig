const std = @import("std");

pub fn main() !void {
    const stdin = std.io.getStdIn().reader();
    const stdout = std.io.getStdOut().writer();
    const windowSize: usize = 14;

    var buffer: [1]u8 = undefined;

    var ring: [windowSize]u5 = undefined;
    var head: usize = 0;
    var tail: usize = 0;
    var ringSize: usize = 0;

    var pos: usize = 0;
    while (true) {
        const read_result = try stdin.read(buffer[0..1]);
        if (read_result == 0) break;
        var ch = buffer[0];
        if ((ch < 'a') or (ch > 'z')) {
            std.debug.print("ignoring invalid input char of value {d}.\n", .{ch});
            continue;
        }
        if (ringSize == windowSize) {
            const one: i32 = 1;
            var bitSet: i32 = 0;
            // I'm too lazy to use a Map<char, count> for this XD
            inline for (ring) |i| {
                bitSet |= one << i;
            }

            if (@popCount(bitSet) == windowSize) {
                try stdout.print("First marker at {d}\n", .{pos});
                break;
            }

            head += 1;
            if (head == windowSize) {
                head = 0;
            }
        } else {
            ringSize += 1;
        }
        // we've already checked ch is between 'a' - 'z'
        ring[tail] = @intCast(u5, ch - 'a');

        tail += 1;
        if (tail == windowSize) {
            tail = 0;
        }
        pos += 1;
    }
}
