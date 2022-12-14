require "bit_array"

draw_commands = [] of Array({x: Int32, y: Int32})
STDIN.each_line do |line|
    points = line.split(" -> ")
        .map{|pair| pair.split(",")}
        .map{|strs| {x: strs[0].to_i, y: strs[1].to_i}}
    draw_commands << points
end

# puts draw_commands
boundary = draw_commands.flatten
    .reduce({min_x: Int32::MAX, min_y: 0, max_x: Int32::MIN, max_y: 0}) { |boundary, point|
    min_x = Math.min(boundary[:min_x], point[:x])
    max_x = Math.max(boundary[:max_x], point[:x])

    # we know min y is 0, so only check for max y.
    max_y = Math.max(boundary[:max_y], point[:y])

    {min_x: min_x, min_y: boundary[:min_y], max_x: max_x, max_y: max_y}
}

puts boundary

cave_minx = boundary[:min_x]
cave_height = boundary[:max_y] + 1
cave_width = boundary[:max_x] - cave_minx + 1

cave_bitmap = BitArray.new(cave_height * cave_width)

draw_commands.each { |cmds_batch|
    cmds_batch.each.cons_pair.each { |from_to|
        from = from_to[0]
        to = from_to[1]
        if from[:x] == to[:x]
            x = from[:x]
            y1, y2 = from[:y], to[:y]
            if y1 > y2
                y1, y2 = y2, y1
            end
            (y1..y2).each {|y| cave_bitmap[y * cave_width + x - cave_minx] = true}
        else
            y = from[:y]
            x1, x2 = from[:x], to[:x]
            if x1 > x2
                x1, x2 = x2, x1
            end
            (x1..x2).each {|x| cave_bitmap[y * cave_width + x - cave_minx] = true}
        end
    }
}

def print_map(bitmap : BitArray, width : Int32, height : Int32)
    (0...height).each {|y| puts bitmap[y * width...(y + 1) * width]}
end

def try_fall (x_offset : Int32, y : Int32, bitmap : BitArray, cave_width : Int32, cave_height : Int32) : {Int32, Int32}
    if y >= cave_height || x_offset < 0 || x_offset >= cave_width
        # fall into the void
        return -2, -2
    end
    if bitmap[y * cave_width + x_offset]
        return -1, -1
    end
    probe_y = y + 1
    [x_offset, x_offset - 1, x_offset + 1].each { |probe_x|
        next_x, next_y = try_fall(probe_x, probe_y, bitmap, cave_width, cave_height)
        if next_x == -2
            # would fall into the void, report back
            return next_x, next_y
        elsif next_x > -1
            return next_x, next_y
        end
    }
    # no place to fall to, comes to rest in the current empty place
    return x_offset, y
end

print_map(cave_bitmap, cave_width, cave_height)
drops = 0
while true
    x, y = try_fall(500 - cave_minx, 0, cave_bitmap, cave_width, cave_height)
    if x > -1
        drops += 1
        # puts "rest at #{x + cave_minx},#{y}"
        cave_bitmap[y * cave_width + x] = true
    else
        break
    end
end

puts "Simulating..."
print_map(cave_bitmap, cave_width, cave_height)
puts "Drops: #{drops}"