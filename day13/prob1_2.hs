import Data.Either (rights)
import Data.List (elemIndex, sort)
import Data.Maybe (fromJust)
import Text.Parsec
import Text.Parsec.String

data List a = Elem a | List [List a]
  deriving (Eq, Show)

type IList = List Integer

listParser :: Parser IList
listParser = do
  char '['
  elements <- sepBy ((Elem <$> integerParser) <|> listParser) (char ',')
  char ']'
  return (List elements)

integerParser :: Parser Integer
integerParser = read <$> (many1 digit)

comparePair :: IList -> IList -> Ordering
comparePair (List []) (List []) = EQ
comparePair (List (_:_)) (List []) = GT
comparePair (List []) (List (_:_)) = LT
comparePair (Elem a) (Elem b) = compare a b
comparePair (List xs) (Elem b) = comparePair (List xs) (List [Elem b])
comparePair (Elem a) (List xs) = comparePair (List [Elem a]) (List xs)
comparePair (List (head1:tail1)) (List (head2:tail2)) =
  let headResult = comparePair head1 head2 in
    if headResult /= EQ
      then headResult
      else comparePair (List tail1) (List tail2)

instance Ord IList where
  compare = comparePair

splitEvery _ [] = []
splitEvery n list = first : (splitEvery n rest)
  where
    (first,rest) = splitAt n list

validatePair :: String -> String -> Either ParseError Bool
validatePair s1 s2 = do
  p1 <- parse listParser "" s1
  p2 <- parse listParser "" s2
  return (p1 < p2)

main = do
  contents <- getContents
  let input = filter (/= "") (lines contents)
  -- problem 1
  let validationResults = map (\(l1:l2:_) -> validatePair l1 l2) (splitEvery 2 input)
  let withIndex = zip validationResults [1..]
  putStrLn . show . sum . map snd . filter (\(Right isValid, _) -> isValid) $ withIndex
  -- problem 2
  let driverP1 = List[List[Elem 2]]
  let driverP2 = List[List[Elem 6]]
  let allPackets = [driverP1, driverP2] ++ rights (map (parse listParser "") input)
  let sorted = sort allPackets
  let p1Idx = fromJust $ (elemIndex driverP1 sorted)
  let p2Idx = fromJust $ (elemIndex driverP2 sorted)
  putStrLn . show $ ((p1Idx + 1) * (p2Idx + 1))
