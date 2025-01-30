package net.mt32.expoll.helper

import kotlin.random.Random

fun generateRandomUsername(): String {
    val adjective = ADJECTIVES[Random.nextInt(ADJECTIVES.size)]
    val noun = NOUNS[Random.nextInt(NOUNS.size)]
    return "$adjective $noun"
}

private val ADJECTIVES = listOf(
    "Able", "Abundant", "Accurate", "Active", "Adventurous", "Agile", "Alert", "Amazing", "Ambitious", "Amusing",
    "Ancient", "Angelic", "Animated", "Appreciative", "Astonishing", "Astute", "Athletic", "Attentive", "Attractive",
    "Authentic", "Aware", "Awesome", "Balanced", "Beautiful", "Beloved", "Benevolent", "Bighearted", "Blazing",
    "Blessed", "Blissful", "Bold", "Bouncy", "Brainy", "Brave", "Bright", "Brilliant", "Bubbly", "Busy", "Calm",
    "Capable", "Carefree", "Careful", "Caring", "Cautious", "Challenging", "Charming", "Cheerful", "Chic",
    "Chivalrous", "Clever", "Colorful", "Comedic", "Compassionate", "Confident", "Conscientious", "Considerate",
    "Cool", "Cooperative", "Courageous", "Creative", "Cultured", "Curious", "Daring", "Dazzling", "Decisive",
    "Dedicated", "Delightful", "Dependable", "Determined", "Dextrous", "Diligent", "Diplomatic", "Discerning",
    "Distinguished", "Divine", "Dramatic", "Dynamic", "Eager", "Earnest", "Easygoing", "Eclectic", "Economic",
    "Ecstatic", "Educated", "Effervescent", "Efficient", "Elegant", "Elevated", "Eloquent", "Empathetic",
    "Empowered", "Enchanting", "Encouraging", "Endearing", "Energetic", "Engaging", "Enlightened", "Entertaining",
    "Enthusiastic", "Epic", "Euphoric", "Eventful", "Everlasting", "Excellent", "Exciting", "Exemplary",
    "Exhilarating", "Exotic", "Expansive", "Experienced", "Expressive", "Extraordinary", "Exuberant", "Fabulous",
    "Fair", "Faithful", "Fantastic", "Fashionable", "Fearless", "Fervent", "Fiery", "Flexible", "Flourishing",
    "Focused", "Forgiving", "Fortunate", "Friendly", "Frisky", "Fulfilling", "Funny", "Gallant", "Generous",
    "Genuine", "Gentle", "Gifted", "Glorious", "Graceful", "Gracious", "Grand", "Grateful", "Great", "Gregarious",
    "Groovy", "Grounded", "Growing", "Handsome", "Hardy", "Harmonious", "Healthy", "Heartfelt", "Hearty",
    "Heavenly", "Helpful", "Heroic", "High-spirited", "Hilarious", "Honest", "Hopeful", "Hospitable", "Humble",
    "Humorous", "Idealistic", "Imaginative", "Impressive", "Incredible", "Independent", "Industrious", "Informed",
    "Innovative", "Insightful", "Inspiring", "Intelligent", "Intrepid", "Inventive", "Inviting", "Involved",
    "Irresistible", "Jazzy", "Jolly", "Jovial", "Joyful", "Jubilant", "Judicious", "Keen", "Kind", "Knowledgeable",
    "Lively", "Logical", "Lovable", "Lovely", "Loyal", "Lucky", "Luminous", "Lush", "Magical", "Magnanimous",
    "Majestic", "Mature", "Mellow", "Merciful", "Merry", "Meticulous", "Mindful", "Mirthful", "Modest", "Motivated",
    "Mysterious", "Noble", "Notable", "Nurturing", "Observant", "Optimistic", "Orderly", "Original", "Outgoing",
    "Outstanding", "Passionate", "Patient", "Peaceful", "Perceptive", "Persistent", "Personable", "Persuasive",
    "Philosophical", "Playful", "Pleasant", "Plucky", "Poised", "Polished", "Popular", "Positive", "Powerful",
    "Practical", "Precious", "Precise", "Prepared", "Productive", "Professional", "Prolific", "Prosperous",
    "Protective", "Proud", "Prudent", "Punctual", "Quaint", "Qualified", "Quirky", "Radiant", "Rational",
    "Reassuring", "Receptive", "Resilient", "Resourceful", "Respected", "Responsible", "Reverent", "Revolutionary",
    "Robust", "Romantic", "Rousing", "Sagacious", "Savvy", "Scholarly", "Scintillating", "Secure", "Selfless",
    "Sensitive", "Serene", "Sharp", "Shining", "Sincere", "Skilled", "Sociable", "Sophisticated", "Sparkling",
    "Spectacular", "Speedy", "Spirited", "Splendid", "Spontaneous", "Sporty", "Stable", "Steady", "Strategic",
    "Striking", "Strong", "Stunning", "Stylish", "Suave", "Successful", "Sunny", "Supportive", "Swift",
    "Sympathetic", "Talented", "Tangible", "Tasteful", "Tenacious", "Terrific", "Thorough", "Thoughtful",
    "Thrilling", "Tidy", "Tolerant", "Tough", "Tranquil", "Trustworthy", "Unbreakable", "Understanding",
    "Unique", "Unstoppable", "Upbeat", "Valiant", "Vast", "Versatile", "Vibrant", "Victorious", "Vigilant",
    "Vigorous", "Virtuous", "Visionary", "Vivacious", "Warm", "Welcoming", "Whimsical", "Wholehearted",
    "Willing", "Wise", "Witty", "Wondrous", "Xenial", "Youthful", "Zany", "Zealous", "Zesty", "Zippy"
)

private val NOUNS = listOf(
    "Acorn", "Adventure", "Aircraft", "Alchemy", "Alpaca", "Ambition", "Anchor", "Angel", "Antelope", "Apple",
    "Apprentice", "Aquarium", "Arch", "Archer", "Artist", "Ash", "Aspen", "Astronaut", "Avalanche", "Avenue",
    "Avocado", "Ballet", "Balloon", "Banana", "Bandit", "Banyan", "Barrel", "Basil", "Beacon", "Bear", "Beaver",
    "Bee", "Beetle", "Bell", "Berry", "Bicycle", "Billow", "Birch", "Bird", "Blizzard", "Blossom", "Bluejay",
    "Boulder", "Bow", "Bravado", "Breeze", "Brick", "Bridge", "Brook", "Bubble", "Bumblebee", "Bunny", "Cabin",
    "Cactus", "Cadence", "Candle", "Canopy", "Canyon", "Caravan", "Cardinal", "Carriage", "Cascade", "Castle",
    "Cat", "Cedar", "Celestial", "Champion", "Chandelier", "Chaparral", "Charm", "Chariot", "Cheetah", "Cherry",
    "Chestnut", "Chimera", "Chime", "Cinnamon", "Cloud", "Clover", "Cobra", "Comet", "Compass", "Concert",
    "Constellation", "Cottage", "Cougar", "Cove", "Crane", "Crater", "Crayon", "Crescendo", "Crescent", "Cricket",
    "Crown", "Cub", "Cupcake", "Current", "Curtain", "Cyclone", "Dagger", "Daisy", "Dancer", "Dawn", "Deer",
    "Desert", "Diamond", "Dingo", "Dinosaur", "Dolphin", "Dome", "Dove", "Dragon", "Drizzle", "Dune", "Eagle",
    "Echo", "Eclipse", "Eel", "Elf", "Ember", "Emerald", "Equinox", "Falcon", "Feather", "Fern", "Festival",
    "Fig", "Firefly", "Fjord", "Flame", "Flamingo", "Flare", "Flash", "Flower", "Fog", "Forest", "Fox", "Fountain",
    "Frost", "Galaxy", "Gale", "Gazelle", "Gem", "Geyser", "Giraffe", "Glacier", "Glade", "Glow", "Goldfinch",
    "Gorilla", "Grape", "Grasshopper", "Griffin", "Grove", "Gull", "Harmony", "Hawk", "Haystack", "Hearth",
    "Hedgehog", "Helix", "Horizon", "Hummingbird", "Hurricane", "Icicle", "Iguana", "Illusion", "Infinity",
    "Island", "Ivory", "Jade", "Jaguar", "Jazz", "Jellyfish", "Journey", "Jubilee", "Jungle", "Kaleidoscope",
    "Kangaroo", "Kelp", "Kestrel", "Key", "Kiwi", "Lagoon", "Lantern", "Lark", "Lavender", "Leaf", "Legend",
    "Lighthouse", "Lightning", "Lion", "Llama", "Lullaby", "Lynx", "Macaw", "Magnet", "Mango", "Maple", "Marble",
    "Mariner", "Meadow", "Melody", "Meteor", "Midnight", "Mimic", "Mirage", "Mirth", "Mist", "Mockingbird",
    "Monarch", "Moon", "Moss", "Mountain", "Murmur", "Mystery", "Nebula", "Nest", "Nightshade", "Nomad",
    "Northstar", "Nova", "Nutmeg", "Oasis", "Ocean", "Olive", "Onyx", "Opal", "Orchid", "Otter", "Owl",
    "Paladin", "Palm", "Panther", "Papaya", "Parade", "Parrot", "Pastry", "Pathfinder", "Peach", "Pegasus",
    "Penguin", "Peony", "Peppermint", "Peridot", "Phoenix", "Piano", "Pine", "Pioneer", "Piranha", "Pirate",
    "Planet", "Plume", "Plover", "Poppy", "Prairie", "Prism", "Puffin", "Puma", "Quail", "Quartz", "Quest",
    "Quill", "Rabbit", "Radiance", "Raven", "Reef", "Reflection", "Reindeer", "Ripple", "River", "Robin",
    "Rocket", "Rose", "Sable", "Saffron", "Sailor", "Sapphire", "Savanna", "Scarab", "Scepter", "Seagull",
    "Sequoia", "Shadow", "Shamrock", "Shell", "Sheriff", "Shooting Star", "Shoreline", "Silhouette", "Sky",
    "Snowfall", "Snowflake", "Solar", "Sparrow", "Specter", "Spectrum", "Speedster", "Sphere", "Spider",
    "Sprout", "Squall", "Squirrel", "Star", "Stargazer", "Storm", "Sunbeam", "Sunflower", "Sunset", "Surf",
    "Swallow", "Symphony", "Talisman", "Tangerine", "Tempest", "Thistle", "Thunder", "Tide", "Tiger", "Topaz",
    "Torrent", "Toucan", "Tower", "Trailblazer", "Treasure", "Treehouse", "Tundra", "Twilight", "Typhoon",
    "Umbra", "Utopia", "Vale", "Vanguard", "Velvet", "Vine", "Violin", "Vortex", "Voyager", "Walnut", "Waterfall",
    "Wave", "Whale", "Whirlwind", "Wildflower", "Willow", "Windchime", "Wolf", "Wren", "Zephyr", "Zodiac"
)