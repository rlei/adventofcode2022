alias Point = {x: Int32, y: Int32}

draw_commands = [] of Array(Point)
STDIN.each_line do |line|
    points = line.split(" -> ")
        .map{|pair| pair.split(",")}
        .map{|strs| {x: strs[0].to_i, y: strs[1].to_i}}
    draw_commands << points
end

max_y = draw_commands.flatten
    .map{|point| point[:y]}
    .max

# our sparse matrix
cave_bitmap = Hash(Point, Bool).new(false)

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
            (y1..y2).each {|y| cave_bitmap[{x: x, y: y}] = true}
        else
            y = from[:y]
            x1, x2 = from[:x], to[:x]
            if x1 > x2
                x1, x2 = x2, x1
            end
            (x1..x2).each {|x| cave_bitmap[{x: x, y: y}] = true}
        end
    }
}

# The slower O(M*N*N), version that simulates every single drop
def try_fall (x : Int32, y : Int32, bitmap : Hash(Point, Bool), max_y : Int32) : {Int32, Int32}
    if y == max_y + 2
        # the added floor is always solid
        return -1, -1
    end
    if bitmap[{x: x, y: y}]
        return -1, -1
    end
    probe_y = y + 1
    [x, x - 1, x + 1].each { |probe_x|
        next_x, next_y = try_fall(probe_x, probe_y, bitmap, max_y)
        if next_x > -1
            return next_x, next_y
        end
    }
    # no place to fall to, comes to rest in the current empty place
    return x, y
end

# The fast O(M*N), counting only version that marks the cave bitmap along the search
def count_fallable (x : Int32, y : Int32, bitmap : Hash(Point, Bool), max_y : Int32) : Int32
    if y == max_y + 2
        # the added floor is always solid
        return 0
    end
    if bitmap[{x: x, y: y}]
        return 0
    end
    drops = 1
    # mark
    bitmap[{x: x, y: y}] = true

    probe_y = y + 1
    [x, x - 1, x + 1].each { |probe_x|
        drops += count_fallable(probe_x, probe_y, bitmap, max_y)
    }
    return drops
end

puts "Drops: #{count_fallable(500, 0, cave_bitmap, max_y)}"

# drops = 0
# while true
#     x, y = try_fall(500, 0, cave_bitmap, max_y)
#     if x > -1
#         drops += 1
#         # puts "rest at #{x + cave_minx},#{y}"
#         cave_bitmap[{x: x, y: y}] = true
#     else
#         break
#     end
# end
# 
# puts "Drops: #{drops}"