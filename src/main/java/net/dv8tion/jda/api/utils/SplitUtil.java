/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.api.utils;

import net.dv8tion.jda.internal.utils.Checks;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utility to strategically split strings.
 */
public class SplitUtil
{
    /**
     * Apply a list of {@link Strategy Strategies} to split the provided string into chunks of a maximum {@code limit} characters.
     * <br>The substring chunks will not be trimmed of whitespace, you can use {@link #split(String, int, boolean, Strategy...)} to trim them.
     *
     * <p>If no strategies are provided, ie. {@code split(string, limit, true)}, then it only uses the limit to split with {@link Strategy#ANYWHERE}.
     *
     * <p>Strategies are applied in order, each trying to split with different criteria.
     * When a strategy fails, the next in the list is tried until all strategies are exhausted.
     * If not a single strategy can split the string, an {@link IllegalStateException} is thrown.
     *
     * @param  input
     *         The input string to split up
     * @param  limit
     *         The maximum string length for each chunk
     * @param  strategies
     *         The split strategies
     *
     * @throws IllegalStateException
     *         If none of the strategies successfully split the string.
     *         You can use {@link Strategy#ANYWHERE} to always split at the limit and avoid this exception.
     *
     * @return {@link List} of each substring which is at most {@code limit} characters long
     *
     * @see    Strategy#ANYWHERE
     * @see    Strategy#NEWLINE
     * @see    Strategy#WHITESPACE
     */
    @Nonnull
    public static List<String> split(@Nonnull String input, int limit, @Nonnull Strategy... strategies)
    {
        return split(input, limit, false, strategies);
    }

    /**
     * Apply a list of {@link Strategy Strategies} to split the provided string into chunks of a maximum {@code limit} characters.
     *
     * <p>If no strategies are provided, ie. {@code split(string, limit, true)}, then it only uses the limit to split with {@link Strategy#ANYWHERE}.
     *
     * <p>Strategies are applied in order, each trying to split with different criteria.
     * When a strategy fails, the next in the list is tried until all strategies are exhausted.
     * If not a single strategy can split the string, an {@link IllegalStateException} is thrown.
     *
     * @param  input
     *         The input string to split up
     * @param  limit
     *         The maximum string length for each chunk
     * @param  trim
     *         Whether to trim the chunks after splitting (See {@link String#trim()})
     * @param  strategies
     *         The split strategies
     *
     * @throws IllegalStateException
     *         If none of the strategies successfully split the string.
     *         You can use {@link Strategy#ANYWHERE} to always split at the limit and avoid this exception.
     *
     * @return {@link List} of each substring which is at most {@code limit} characters long
     *
     * @see    Strategy#ANYWHERE
     * @see    Strategy#NEWLINE
     * @see    Strategy#WHITESPACE
     */
    @Nonnull
    public static List<String> split(@Nonnull String input, int limit, boolean trim, @Nonnull Strategy... strategies)
    {
        Checks.notNull(input, "Input string");
        if (input.isEmpty() || input.length() <= limit)
            return Collections.singletonList(input);
        if (strategies.length == 0)
            strategies = new Strategy[] { Strategy.ANYWHERE };
        int offset = 0;
        List<String> chunks = new LinkedList<>();

        while (offset < input.length())
        {
            String chunk = null;

            if (input.length() - offset <= limit)
            {
                chunk = input.substring(offset);
                offset = input.length();
            }
            else
            {
                for (Strategy strategy : strategies)
                {
                    int newOffset = strategy.apply(input, offset, limit);
                    if (newOffset > offset)
                    {
                        newOffset = Math.min(newOffset, input.length());
                        chunk = input.substring(offset, newOffset);
                        offset = newOffset;
                        break;
                    }
                }
            }

            if (chunk == null)
                throw new IllegalStateException("None of the strategies successfully split the string. Try adding Strategy.ANYWHERE to the end of your strategy list.");
            if (trim)
                chunk = chunk.trim();
            if (chunk.isEmpty())
                continue;
            chunks.add(chunk);
        }

        return chunks;
    }

    /**
     * Function which applies a programmable strategy used to determine a splitting point.
     *
     * @see #ANYWHERE
     * @see #NEWLINE
     * @see #WHITESPACE
     */
    public interface Strategy
    {
        /**
         * Implements a splitting strategy.
         *
         * <p>The goal of a strategy is to implement a greedy algorithm to find the optimal point to split the string.
         * Ideally, this should be close to the {@code limit}.
         *
         * <p>This should not return an offset larger than {@code limit}.
         * Any offset lower than the input offset, is interpreted as unsuccessful.
         *
         * @param  string
         *         The input string
         * @param  offset
         *         The current offset where to start your substring
         * @param  limit
         *         The maximum length your substring should be
         *
         * @return The exclusive end index of your chunk, negative to indicate failure. (should be in range of {@code offset < x <= limit}).
         */
        int apply(@Nonnull String string, int offset, int limit);

        /**
         * Strategy which splits at any character to satisfy the limit.
         * <br>This is the default strategy if none is provided, and should ideally only be the final one in your list.
         */
        Strategy ANYWHERE = (string, offset, limit) -> Math.min(string.length(), offset + limit);
        /**
         * Splits on newline characters. Specifically on {@code '\n'}.
         */
        Strategy NEWLINE = onChar('\n');
        /**
         * Splits on every character which is considered {@link Character#isWhitespace(char) whitespace}.
         */
        Strategy WHITESPACE = onChar(Character::isWhitespace);

        /**
         * Strategy to split on the provided character.
         *
         * <p>An example use-case would be to define places via {@code '\0'} and then splitting exactly on those.
         *
         * @param  c
         *         The splitting character
         *
         * @return The strategy to split on that character
         */
        @Nonnull
        static Strategy onChar(char c)
        {
            return (string, offset, limit) ->
            {
                int result = -1;
                do
                {
                    int index = string.indexOf(c, Math.max(result + 1, offset));
                    if (index == -1)
                        break;
                    result = index;
                } while (result < offset + limit);
                return result;
            };
        }

        /**
         * Strategy to split on the provided character tests.
         *
         * @param  predicate
         *         The splitting character test
         *
         * @throws IllegalArgumentException
         *         If the predicate is null
         *
         * @return The strategy to split on characters that pass the test
         */
        @Nonnull
        static Strategy onChar(@Nonnull Predicate<Character> predicate)
        {
            Checks.notNull(predicate, "Predicate");
            return (string, offset, limit) ->
            {
                int current = -1;
                for (int i = offset; i < offset + limit; i++)
                {
                    if (predicate.test(string.charAt(i)))
                        current = i;
                }
                return current;
            };
        }
    }
}