import React from 'react';
import { 
  HomeContainer, 
  HeroSection, 
  HeroContent, 
  HeroTitle, 
  HeroSubtitle,
  FeaturedSection,
  SectionTitle, 
  MovieGrid, 
  MovieCard, 
  MovieImage, 
  MovieInfo, 
  MovieTitle, 
  MovieRating, 
  CategoriesSection, 
  CategoryGrid, 
  CategoryCard, 
  Button 
} from '../styles/HomepageStyle';

const Home: React.FC = () => {
  // Mock data for featured movies
  const featuredMovies = [
    {
      id: 1,
      title: "The Dark Knight",
      rating: 9.0,
      image: "/images/movies/dark-knight.jpg"
    },
    {
      id: 2,
      title: "Inception",
      rating: 8.8,
      image: "/images/movies/inception.jpg"
    },
    {
      id: 3,
      title: "Interstellar",
      rating: 8.6,
      image: "/images/movies/interstellar.jpg"
    },
    // Add more movies as needed
  ];

  // Mock data for categories
  const categories = [
    "Action", "Drama", "Comedy", "Sci-Fi", "Horror", "Romance",
    "Documentary", "Animation", "Thriller", "Family"
  ];

  return (
    <HomeContainer>
      <HeroSection>
        <HeroContent>
          <HeroTitle>Welcome to BBMovie</HeroTitle>
          <HeroSubtitle>
            Discover the best movies and TV shows. Start your cinematic journey today!
          </HeroSubtitle>
          <Button to="/movies">Browse Movies</Button>
        </HeroContent>
      </HeroSection>

      <FeaturedSection>
        <SectionTitle>Featured Movies</SectionTitle>
        <MovieGrid>
          {featuredMovies.map((movie) => (
            <MovieCard key={movie.id}>
              <MovieImage src={movie.image} alt={movie.title} />
              <MovieInfo>
                <MovieTitle>{movie.title}</MovieTitle>
                <MovieRating>★ {movie.rating}</MovieRating>
              </MovieInfo>
            </MovieCard>
          ))}
        </MovieGrid>
      </FeaturedSection>

      <CategoriesSection>
        <SectionTitle>Browse by Category</SectionTitle>
        <CategoryGrid>
          {categories.map((category) => (
            <CategoryCard key={category} to={`/category/${category.toLowerCase()}`}>
              {category}
            </CategoryCard>
          ))}
        </CategoryGrid>
      </CategoriesSection>
    </HomeContainer>
  );
};

export default Home; 