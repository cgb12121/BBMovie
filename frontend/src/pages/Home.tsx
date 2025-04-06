import React from 'react';
import { Link } from 'react-router-dom';
import styled from 'styled-components';

const HomeContainer = styled.div`
  min-height: 100vh;
  background-color: #1a1a1a;
  color: #ffffff;
`;

const HeroSection = styled.div`
  height: 80vh;
  background: linear-gradient(rgba(0, 0, 0, 0.7), rgba(0, 0, 0, 0.7)),
              url('/images/hero-bg.jpg') center/cover;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 0 20px;
`;

const HeroContent = styled.div`
  max-width: 800px;
`;

const HeroTitle = styled.h1`
  font-size: 4rem;
  margin-bottom: 1rem;
  color: #ffffff;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
`;

const HeroSubtitle = styled.p`
  font-size: 1.5rem;
  margin-bottom: 2rem;
  color: #cccccc;
`;

const Button = styled(Link)`
  display: inline-block;
  padding: 1rem 2rem;
  background-color: #e50914;
  color: #ffffff;
  text-decoration: none;
  border-radius: 4px;
  font-weight: bold;
  transition: background-color 0.3s;

  &:hover {
    background-color: #f40612;
  }
`;

const FeaturedSection = styled.section`
  padding: 4rem 2rem;
`;

const SectionTitle = styled.h2`
  font-size: 2rem;
  margin-bottom: 2rem;
  color: #ffffff;
`;

const MovieGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 2rem;
  padding: 0 2rem;
`;

const MovieCard = styled.div`
  background-color: #2a2a2a;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s;

  &:hover {
    transform: translateY(-5px);
  }
`;

const MovieImage = styled.img`
  width: 100%;
  height: 300px;
  object-fit: cover;
`;

const MovieInfo = styled.div`
  padding: 1rem;
`;

const MovieTitle = styled.h3`
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
  color: #ffffff;
`;

const MovieRating = styled.div`
  color: #ffd700;
  font-weight: bold;
`;

const CategoriesSection = styled.section`
  padding: 4rem 2rem;
  background-color: #2a2a2a;
`;

const CategoryGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 1rem;
  padding: 0 2rem;
`;

const CategoryCard = styled(Link)`
  background-color: #3a3a3a;
  padding: 1rem;
  border-radius: 4px;
  text-align: center;
  text-decoration: none;
  color: #ffffff;
  transition: background-color 0.3s;

  &:hover {
    background-color: #4a4a4a;
  }
`;

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