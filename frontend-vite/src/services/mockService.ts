import type { MockMovie, MockCategory, MockApiResponse } from '../types/mockData';

class MockService {
  private movies: MockMovie[] = [
    {
      id: 1,
      title: "The Dark Knight",
      description: "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.",
      rating: 9.0,
      posterUrl: "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
      backdropUrl: "https://image.tmdb.org/t/p/w1280/tFKz4pJq5UcugpaZSckPg0MZlGM.jpg",
      releaseDate: "2008-07-18",
      duration: "2h 32m",
      genres: ["Action", "Crime", "Drama"],
      director: "Christopher Nolan",
      cast: ["Christian Bale", "Heath Ledger", "Aaron Eckhart", "Michael Caine"],
      year: 2008
    },
    {
      id: 2,
      title: "Inception",
      description: "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.",
      rating: 8.8,
      posterUrl: "https://image.tmdb.org/t/p/w500/oYuLEt3zVJiY6cI4cMMuK2qgEUw.jpg",
      backdropUrl: "https://image.tmdb.org/t/p/w1280/fllxxkH1mvNyjvQ5w534pZ8mtl.jpg",
      releaseDate: "2010-07-16",
      duration: "2h 28m",
      genres: ["Action", "Sci-Fi", "Thriller"],
      director: "Christopher Nolan",
      cast: ["Leonardo DiCaprio", "Marion Cotillard", "Tom Hardy", "Ellen Page"],
      year: 2010
    },
    {
      id: 3,
      title: "The Shawshank Redemption",
      description: "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.",
      rating: 9.3,
      posterUrl: "https://image.tmdb.org/t/p/w500/q6y0Go1tsGEsmtFryDOJo3dEYpE.jpg",
      backdropUrl: "https://image.tmdb.org/t/p/w1280/xBKGJQsAIeweesB79KC89FpBrVr.jpg",
      releaseDate: "1994-09-23",
      duration: "2h 22m",
      genres: ["Drama"],
      director: "Frank Darabont",
      cast: ["Tim Robbins", "Morgan Freeman", "Bob Gunton", "William Sadler"],
      year: 1994
    },
    {
      id: 4,
      title: "Pulp Fiction",
      description: "The lives of two mob hitmen, a boxer, a gangster and his wife intertwine in four tales of violence and redemption.",
      rating: 8.9,
      posterUrl: "https://image.tmdb.org/t/p/w500/dM2w364MzkRHhnZNCHSnMO8co8f.jpg",
      backdropUrl: "https://image.tmdb.org/t/p/w1280/suaEOtk1N1sgg2MTM7oZd2cfVp3.jpg",
      releaseDate: "1994-10-14",
      duration: "2h 34m",
      genres: ["Crime", "Drama"],
      director: "Quentin Tarantino",
      cast: ["John Travolta", "Uma Thurman", "Samuel L. Jackson", "Bruce Willis"],
      year: 1994
    },
    {
      id: 5,
      title: "Forrest Gump",
      description: "The presidencies of Kennedy and Johnson, the events of Vietnam, Watergate, and other historical events unfold through the perspective of an Alabama man with an IQ of 75.",
      rating: 8.8,
      posterUrl: "https://image.tmdb.org/t/p/w500/yE5d3bu6WgE6fkn8a37AcjufnhD.jpg",
      backdropUrl: "https://image.tmdb.org/t/p/w1280/ctOEhQi1h83gHsW6R1QW7n9ic3U.jpg",
      releaseDate: "1994-07-06",
      duration: "2h 22m",
      genres: ["Drama", "Romance"],
      director: "Robert Zemeckis",
      cast: ["Tom Hanks", "Robin Wright", "Gary Sinise", "Sally Field"],
      year: 1994
    },
    {
      id: 6,
      title: "The Matrix",
      description: "A computer hacker learns from mysterious rebels about the true nature of his reality and his role in the war against its controllers.",
      rating: 8.7,
      posterUrl: "https://image.tmdb.org/t/p/w500/hEpWvX6BpX09Oyh5CMPSmvAh1Lb.jpg",
      backdropUrl: "https://image.tmdb.org/t/p/w1280/bb9RdJ7kNq6H8F8GyK5B9K5j7NS.jpg",
      releaseDate: "1999-03-31",
      duration: "2h 16m",
      genres: ["Action", "Sci-Fi"],
      director: "Lana Wachowski, Lilly Wachowski",
      cast: ["Keanu Reeves", "Laurence Fishburne", "Carrie-Anne Moss", "Hugo Weaving"],
      year: 1999
    },
    {
      id: 7,
      title: "Goodfellas",
      description: "The story of Henry Hill and his life in the mob, covering his relationship with his wife Karen Hill and his mob partners Jimmy Conway and Tommy DeVito.",
      rating: 8.7,
      posterUrl: "https://image.tmdb.org/t/p/w500/aKuFiU82s5ISJpGZp7YkIr3kCUd.jpg",
      backdropUrl: "https://image.tmdb.org/t/p/w1280/6UFXGbq8hbewm8Mb4SphR18zuwY.jpg",
      releaseDate: "1990-09-12",
      duration: "2h 26m",
      genres: ["Biography", "Crime", "Drama"],
      director: "Martin Scorsese",
      cast: ["Robert De Niro", "Ray Liotta", "Joe Pesci", "Lorraine Bracco"],
      year: 1990
    },
    {
      id: 8,
      title: "Interstellar",
      description: "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.",
      rating: 8.6,
      posterUrl: "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
      backdropUrl: "https://image.tmdb.org/t/p/w1280/xu9zaAevzQ5nnrsXN6JcahLUFFi.jpg",
      releaseDate: "2014-11-05",
      duration: "2h 49m",
      genres: ["Adventure", "Drama", "Sci-Fi"],
      director: "Christopher Nolan",
      cast: ["Matthew McConaughey", "Anne Hathaway", "Jessica Chastain", "Michael Caine"],
      year: 2014
    },
    {
      id: 9,
      title: "Parasite",
      description: "Greed and class discrimination threaten the newly formed symbiotic relationship between the wealthy Park family and the destitute Kim clan.",
      rating: 8.6,
      posterUrl: "https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg",
      backdropUrl: "https://image.tmdb.org/t/p/w1280/mBBgYwKsVQWLq2v0QhGJ054h79H.jpg",
      releaseDate: "2019-05-30",
      duration: "2h 12m",
      genres: ["Comedy", "Drama", "Thriller"],
      director: "Bong Joon Ho",
      cast: ["Song Kang-ho", "Lee Sun-kyun", "Cho Yeo-jeong", "Choi Woo-shik"],
      year: 2019
    },
    {
      id: 10,
      title: "The Godfather",
      description: "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son.",
      rating: 9.2,
      posterUrl: "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
      backdropUrl: "https://image.tmdb.org/t/p/w1280/loRmzZUHdkwl7u0Y831FyiqOu7.jpg",
      releaseDate: "1972-03-15",
      duration: "2h 55m",
      genres: ["Crime", "Drama"],
      director: "Francis Ford Coppola",
      cast: ["Marlon Brando", "Al Pacino", "James Caan", "Diane Keaton"],
      year: 1972
    }
  ];

  private categories: MockCategory[] = [
    {
      id: 1,
      name: "Action",
      description: "High-octane action packed movies with thrilling sequences and explosive scenes.",
      image: "https://image.tmdb.org/t/p/w1280/fIotZDG10hKkXJPPaJ44D3z2J4.jpg"
    },
    {
      id: 2,
      name: "Drama",
      description: "Emotionally charged stories that explore human experiences and relationships.",
      image: "https://image.tmdb.org/t/p/w1280/1Dk3RJj3Qh5M1jJU3mzPZ1C4Vp.jpg"
    },
    {
      id: 3,
      name: "Sci-Fi",
      description: "Science fiction movies that explore futuristic worlds and advanced technology.",
      image: "https://image.tmdb.org/t/p/w1280/6MKU3N2jv69M8JqU8w19VQccUYA.jpg"
    },
    {
      id: 4,
      name: "Comedy",
      description: "Light-hearted and funny movies that bring joy and laughter.",
      image: "https://image.tmdb.org/t/p/w1280/54nZyqQlC2YJgNEfVzLHnD9729.jpg"
    },
    {
      id: 5,
      name: "Horror",
      description: "Scary and suspenseful movies that send chills down your spine.",
      image: "https://image.tmdb.org/t/p/w1280/72diYJBobJ5P81H4lkq4H2P0XGA.jpg"
    },
    {
      id: 6,
      name: "Romance",
      description: "Heartwarming and passionate love stories.",
      image: "https://image.tmdb.org/t/p/w1280/3Jct8JAhHqQw5x0J8r5M2YJcY4.jpg"
    }
  ];

  private delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

  async getMovies(): Promise<MockApiResponse<MockMovie[]>> {
    // Simulate API call delay
    await this.delay(500);
    
    return {
      success: true,
      data: this.movies,
      message: null,
      errors: null
    };
  }

  async getMovieById(id: number | string): Promise<MockApiResponse<MockMovie>> {
    await this.delay(300);
    
    const movie = this.movies.find(m => m.id === Number(id));
    
    if (!movie) {
      return {
        success: false,
        data: null as any,
        message: 'Movie not found',
        errors: ['Movie not found']
      };
    }
    
    return {
      success: true,
      data: movie,
      message: null,
      errors: null
    };
  }

  async getCategories(): Promise<MockApiResponse<MockCategory[]>> {
    await this.delay(400);
    
    return {
      success: true,
      data: this.categories,
      message: null,
      errors: null
    };
  }

  async getMoviesByCategory(categoryId: number | string): Promise<MockApiResponse<MockMovie[]>> {
    await this.delay(500);
    
    // In a real implementation, we'd filter movies by category
    // For mock data, let's return a random selection of movies
    const filteredMovies = this.movies.slice(0, 6);
    
    return {
      success: true,
      data: filteredMovies,
      message: null,
      errors: null
    };
  }

  async searchMovies(query: string): Promise<MockApiResponse<MockMovie[]>> {
    await this.delay(600);
    
    if (!query) {
      return {
        success: true,
        data: [],
        message: null,
        errors: null
      };
    }
    
    const filteredMovies = this.movies.filter(movie => 
      movie.title.toLowerCase().includes(query.toLowerCase()) ||
      (movie.description && movie.description.toLowerCase().includes(query.toLowerCase())) ||
      (movie.genres && movie.genres.some(genre => genre.toLowerCase().includes(query.toLowerCase())))
    );
    
    return {
      success: true,
      data: filteredMovies,
      message: null,
      errors: null
    };
  }

  async getSimilarMovies(movieId: number | string): Promise<MockApiResponse<MockMovie[]>> {
    await this.delay(500);
    
    // Get a few random movies as "similar" movies
    const shuffled = [...this.movies].sort(() => 0.5 - Math.random());
    const similarMovies = shuffled.slice(0, 6).filter(movie => movie.id !== Number(movieId));
    
    return {
      success: true,
      data: similarMovies,
      message: null,
      errors: null
    };
  }

  // Simulate health check
  async healthCheck(): Promise<boolean> {
    await this.delay(100);
    return true;
  }
}

export const mockService = new MockService();