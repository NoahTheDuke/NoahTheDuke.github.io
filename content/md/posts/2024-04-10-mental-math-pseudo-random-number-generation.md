{:title "Mental Math Pseudo Random Number Generation"
 :date "2024-04-10T04:39:42.722Z"
 :tags ["cohost mirror" "mental math" "pseudo random number generation" "pseudo rng" "prng" "rng" "random number generation" "programming"]
 :cohost-url "5500275-mental-math-pseudo-r"}

\> given a large prime p, a multiplier m, and a seed number x_0, a Lehmer PRNG can be defined by the recursive function x_{n+1} = m \cdot x_n \mod p.

\> One pair choice is p=59, m=6. Because 60 \equiv 1 \mod 59, each iteration is a simple matter of multiplying the ones digit by 6 and adding the tens digit. To illustrate, a sequence starting from 17 would continue 43, 22, 14, 25, 32, 15, 31, 9, 54, 29, ..., and taking just the unit's digit, that turns into 3, 2, 4, 5, 2, 5, 1, 9, 4, 9, which looks pretty random.

\> Next up is p=101, m=50. Because p=101, this construction has the advantage that the sequence goes from 1 to 100, making the distribution of the output stream uniform over 1 to 10 and providing a longer period. The choice of multiplier also simplifies computation: if the current state x_i is even, the next number x_{i+1} = 101 - \frac{x_i}{2}, and if odd, x_{i+1} = 50 - \frac{x_i-1}{2}. Not as nice as the previous example, but still not bad.

https://blog.yunwilliamyu.net/2011/08/14/mindhack-mental-math-pseudo-random-number-generators/
