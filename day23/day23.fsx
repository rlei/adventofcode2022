open System

let rec readLines () = seq {
    let line = System.Console.ReadLine()
    if line <> null then
        yield line
        yield! readLines ()
}

type Move =
    | Yaxis of int
    | Xaxis of int

let around = [(-1, -1); (0, -1); (1, -1); (-1, 0); (1, 0); (-1, 1); (0, 1); (1, 1)]

let tryMove move (locationSet: Set<int * int>) ((x, y)) =
    if around
        |> Seq.filter (fun (dx, dy) -> locationSet.Contains((x + dx, y + dy)))
        |> Seq.isEmpty
    then None
    else
        match move with
        | Yaxis(dy) ->
            let toY = y + dy
            if locationSet.Contains((x - 1, toY)) || locationSet.Contains((x, toY)) || locationSet.Contains((x + 1, toY))
            then None else Some(x, toY)
        | Xaxis(dx) ->
            let toX = x + dx
            if locationSet.Contains((toX, y - 1)) || locationSet.Contains((toX, y)) || locationSet.Contains((toX, y + 1))
            then None else Some(toX, y)

let tryMoveNorth = tryMove (Yaxis -1) 
let tryMoveSouth = tryMove (Yaxis +1) 
let tryMoveWest = tryMove (Xaxis -1) 
let tryMoveEast = tryMove (Xaxis +1) 

let movePlans = [tryMoveNorth; tryMoveSouth; tryMoveWest; tryMoveEast]

let elvesMap =
    readLines()
    |> Seq.mapi (fun y line -> line |> Seq.mapi (fun x c -> ((x, y), c)))
    |> Seq.concat
    |> Seq.filter (fun (x, y) -> snd (x, y) = '#')
    |> Seq.map(fst)
    |> Set.ofSeq

let applyMoves locationSet moves =
    let froms = moves |> Seq.map fst |> Set.ofSeq
    let tos = moves |> Seq.map snd |> Set.ofSeq
    Set.difference locationSet froms |> Set.union tos
    
let rec moveAround locationSet movePlans maxRounds executedRounds =
    printf "."
    if maxRounds = 0 then (locationSet, executedRounds) else
    let validMoves=
         locationSet
        |> Seq.map (fun origCoords ->
            movePlans
            |> Seq.map (fun plan -> plan locationSet origCoords) 
            |> Seq.choose id
            |> Seq.map (fun newCoords -> (origCoords, newCoords))
            |> Seq.tryHead)
            //Option.map (fun newCoords -> (origCoords, newCoords)))
        |> Seq.choose id
        |> Seq.groupBy snd
        |> Seq.filter (fun (_newCoords, moves)-> Seq.length(moves) = 1)
        |> Seq.map (fun (_, origOnes) -> Seq.head origOnes)
    if Seq.isEmpty validMoves then (locationSet, executedRounds+1)
    else
        let newMovePlans = (List.tail movePlans) @ [List.head movePlans]
        moveAround (applyMoves locationSet validMoves) newMovePlans (maxRounds - 1) (executedRounds + 1)

let args = Environment.GetCommandLineArgs()

let rounds = if Seq.contains("-2") args then -1 else 10

let (afterMove, executed) = moveAround elvesMap movePlans rounds  0

// afterMove |> Seq.iter (printfn "%A")

let minX = afterMove |> Seq.map fst |> Seq.min
let maxX = afterMove |> Seq.map fst |> Seq.max
let minY = afterMove |> Seq.map snd |> Seq.min
let maxY = afterMove |> Seq.map snd |> Seq.max

printfn "\nTiles %d after %d rounds" ((maxY - minY + 1) * (maxX - minX + 1) - (Seq.length afterMove)) executed
